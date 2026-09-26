package io.chronicle.platform;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class DomainRecord implements Serializable {
    private String createBy, updateBy, remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String v) {
        createBy = v;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String v) {
        updateBy = v;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String v) {
        remark = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        createTime = v;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date v) {
        updateTime = v;
    }
}
