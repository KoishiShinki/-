"""Generate a local MySQL migration plan. Does not connect to or read any database."""
import argparse
import re
from pathlib import Path

TABLES = (
    'tl_worldline', 'tl_user_profile', 'tl_screenshot', 'tl_stage', 'tl_event',
    'tl_event_relation', 'tl_nation_state', 'tl_character', 'tl_chapter',
    'tl_illustration', 'tl_ai_task', 'tl_public_action',
)

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source-db', required=True, help='Name of the local legacy database')
parser.add_argument('--target-db', default='chronicle')
parser.add_argument('--output', required=True, type=Path, help='Private local SQL plan path outside this repository')
args = parser.parse_args()
for name in (args.source_db, args.target_db):
    if not re.fullmatch(r'[A-Za-z0-9_]{1,64}', name):
        parser.error('Database names must contain only ASCII letters, digits and underscores.')
if args.source_db == args.target_db:
    parser.error('Source and target databases must be different.')
root = Path(__file__).resolve().parents[1]
output = args.output.resolve()
if output.is_relative_to(root):
    parser.error('Write the plan outside the publishable repository.')
schema = (root / 'backend/src/main/resources/schema.sql').read_text(encoding='utf-8')
source = '`' + args.source_db + '`'
target = '`' + args.target_db + '`'
statements = [
    '-- PRIVATE LOCAL MIGRATION PLAN: review and back up both databases before execution.',
    '-- Stop old and new applications. Initialize target schema first; target must be empty.',
    '-- AI is disabled during migration. All legacy accounts migrate as USER.',
    f'USE {target};',
    'DELIMITER $$',
    'CREATE PROCEDURE chronicle_migrate_legacy()',
    'BEGIN',
    '  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;',
    '  START TRANSACTION;',
]
for table in ('app_user', 'app_session', 'app_media') + TABLES:
    statements += [
        f'  IF EXISTS (SELECT 1 FROM {target}.`{table}` LIMIT 1) THEN',
        "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target database is not empty; migration cancelled';",
        '  END IF;',
    ]
statements += [
    f'  INSERT INTO {target}.app_user (user_id,user_name,nick_name,password_hash,role,enabled,avatar,created_at)',
    "  SELECT user_id,user_name,COALESCE(NULLIF(nick_name,''),user_name),password,'USER',",
    "         (status='0' AND del_flag='0'),avatar,COALESCE(create_time,CURRENT_TIMESTAMP)",
    f'  FROM {source}.sys_user;',
]
for table in TABLES:
    definition = re.search(r'CREATE TABLE IF NOT EXISTS\s+`?' + table + r'`?\s*\((.*?)\n\);', schema, re.S | re.I)
    if not definition:
        parser.error(f'Cannot find the target schema for {table}.')
    columns = re.findall(r'^\s*`([a-z_]+)`\s+', definition.group(1), re.M)
    if not columns:
        parser.error(f'Cannot extract columns for {table}.')
    names = ','.join('`' + col + '`' for col in columns)
    statements.append(f'  INSERT INTO {target}.`{table}` ({names}) SELECT {names} FROM {source}.`{table}`;')
media_queries = []
media_queries.append(
    "SELECT avatar AS url,user_id AS owner_user_id FROM "
    f"{target}.app_user WHERE avatar LIKE '/profile/%'"
)
for table, column in [('tl_worldline', 'cover_url'), ('tl_screenshot', 'image_url'),
                      ('tl_illustration', 'image_url'), ('tl_character', 'portrait_url')]:
    media_queries.append(f"SELECT x.`{column}` AS url,u.user_id AS owner_user_id FROM {target}.`{table}` x LEFT JOIN {target}.app_user u ON u.user_name=x.create_by WHERE x.`{column}` LIKE '/profile/%'")
for column in ('avatar_url', 'homepage_cover_url'):
    media_queries.append(f"SELECT p.`{column}` AS url,u.user_id AS owner_user_id FROM {target}.tl_user_profile p LEFT JOIN {target}.app_user u ON u.user_id=p.user_id WHERE p.`{column}` LIKE '/profile/%'")
media_union = '\n    UNION ALL\n    '.join(media_queries)
statements += [
    f'  IF EXISTS (SELECT 1 FROM ({media_union}) candidates WHERE owner_user_id IS NULL) THEN',
    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Media reference has no resolvable owner; review local backup before migration';",
    '  END IF;',
    f'  IF EXISTS (SELECT url FROM ({media_union}) candidates GROUP BY url HAVING COUNT(DISTINCT owner_user_id)>1) THEN',
    "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Conflicting media ownership; resolve references in a local backup before migration';",
    '  END IF;',
    f'  INSERT INTO {target}.app_media(url,owner_user_id)',
    f'    SELECT url,MIN(owner_user_id) FROM ({media_union}) candidates GROUP BY url;',
]
statements += [
    '  COMMIT;', 'END$$', 'DELIMITER ;', 'CALL chronicle_migrate_legacy();',
    'DROP PROCEDURE chronicle_migrate_legacy;',
    '-- Review and explicitly promote the intended administrator locally after migration.',
    '-- Copy the legacy media directory into STORAGE_ROOT; keep it outside the repository.',
]
output.parent.mkdir(parents=True, exist_ok=True)
with output.open('x', encoding='utf-8', newline='\n') as file:
    file.write('\n'.join(statements) + '\n')
print(f'Created {output.name}; no database was contacted and no user data was read.')
