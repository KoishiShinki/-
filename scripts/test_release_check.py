"""Regression tests for release exclusions without real credentials."""
import tempfile
import unittest
from pathlib import Path
from release_check import inspect


class ReleaseCheckTests(unittest.TestCase):
    def check_content(self, filename, content):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / filename
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf-8')
            return inspect(root)[1]

    def test_empty_environment_reference_is_publishable(self):
        self.assertEqual([], self.check_content('.env.example', 'AI_API_KEY=\nDB_PASSWORD=\n'))
        self.assertEqual([], self.check_content('backend/src/main/resources/application.yml',
                                               'password: ${DB_PASSWORD:}\n'))
        self.assertEqual([], self.check_content('compose.yml',
                                               'DB_PASSWORD: ${DB_PASSWORD:?Set a local value}\nADMIN_PASSWORD: ${ADMIN_PASSWORD:-}\n'))

    def test_literal_secret_and_secret_fallback_are_rejected(self):
        for content in ['secret: synthetic-value\n', 'token: synthetic-value\n',
                        'password: ${DB_PASSWORD:synthetic-value}\n']:
            with self.subTest(content=content):
                self.assertTrue(self.check_content('backend/src/main/resources/application.yml', content))

    def test_database_data_is_rejected(self):
        self.assertTrue(self.check_content('backend/src/main/resources/db/schema.sql',
                                          "INSERT INTO example VALUES (1);\n"))

    def test_private_and_unknown_files_are_rejected(self):
        for filename in ['.env', 'data/user.json', 'backup.sqlite', 'customer.csv', 'notes.json']:
            with self.subTest(filename=filename):
                self.assertTrue(self.check_content(filename, '{}'))

    def test_utf8_decodable_binary_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / 'frontend/src/image.bin'
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'\x00synthetic')
            findings = inspect(root)[1]
            self.assertTrue(any(label == 'unreviewed binary' for _, _, label in findings))

    def test_symlinked_directory_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / 'outside'
            target.mkdir()
            link = root / 'frontend' / 'src' / 'linked'
            link.parent.mkdir(parents=True)
            try:
                link.symlink_to(target, target_is_directory=True)
            except (OSError, NotImplementedError):
                self.skipTest('directory symlinks are unavailable')
            findings = inspect(root)[1]
            self.assertTrue(any(path == 'frontend/src/linked' and 'symlink' in label
                                for path, _, label in findings))

    def test_detected_secret_value_is_not_in_findings(self):
        credential = 'sk-' + 'a' * 32
        findings = self.check_content('frontend/src/example.ts', f'const value = "{credential}"')
        self.assertTrue(findings)
        self.assertNotIn(credential, str(findings))


if __name__ == '__main__':
    unittest.main()
