package io.chronicle.timeline.domain;

import java.io.Serializable;
import java.util.Date;

public class TlUserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long profileId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String creatorTitle;
    private String homepageCoverUrl;
    private Date createTime;
    private Date updateTime;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCreatorTitle() {
        return creatorTitle;
    }

    public void setCreatorTitle(String creatorTitle) {
        this.creatorTitle = creatorTitle;
    }

    public String getHomepageCoverUrl() {
        return homepageCoverUrl;
    }

    public void setHomepageCoverUrl(String homepageCoverUrl) {
        this.homepageCoverUrl = homepageCoverUrl;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
