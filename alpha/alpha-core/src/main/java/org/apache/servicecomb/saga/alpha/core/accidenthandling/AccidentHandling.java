/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.accidenthandling;

import com.google.gson.GsonBuilder;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Accident Handling.
 *
 * @author Gannalyo
 * @since 2019/06/14
 */
@Entity
@Table(name = "Accident")
public class AccidentHandling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String servicename;
    private String instanceid;
    private String globaltxid;
    private String localtxid;

    // 1-rollback_error, 2-send_msg_error
    private int type;
    // 0-init, 1-sending, 2-success, 3-fail
    private int status;
    private String bizinfo;
    private String remark;
    private Date createtime;
    private Date completetime;

    private AccidentHandling() {
    }

    public AccidentHandling(String servicename, String instanceid, String globaltxid, String localtxid, AccidentHandleType type, String bizinfo, String remark) {
        this.servicename = servicename;
        this.instanceid = instanceid;
        this.globaltxid = globaltxid;
        this.localtxid = localtxid;
        this.type = type.toInteger();
        this.status = AccidentHandleStatus.SENDING.toInteger();
        this.bizinfo = bizinfo;
        this.remark = remark;
        this.createtime = new Date(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public String getInstanceid() {
        return instanceid;
    }

    public void setInstanceid(String instanceid) {
        this.instanceid = instanceid;
    }

    public String getGlobaltxid() {
        return globaltxid;
    }

    public void setGlobaltxid(String globaltxid) {
        this.globaltxid = globaltxid;
    }

    public String getLocaltxid() {
        return localtxid;
    }

    public void setLocaltxid(String localtxid) {
        this.localtxid = localtxid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getBizinfo() {
        return bizinfo;
    }

    public void setBizinfo(String bizinfo) {
        this.bizinfo = bizinfo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getCompletetime() {
        return completetime;
    }

    public void setCompletetime(Date completetime) {
        this.completetime = completetime;
    }

    public String toJsonString() {
        return new GsonBuilder().create().toJson(this);
    }

    public Map<String, Object> toMap(String typeDesc, String statusDesc) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("servicename", this.servicename);
        map.put("instanceid", this.instanceid);
        map.put("globaltxid", this.globaltxid);
        map.put("localtxid", this.localtxid);
        map.put("type_db", this.type);
        map.put("type", typeDesc);
        map.put("status_db", this.status);
        map.put("status", statusDesc);
        map.put("bizinfo", this.bizinfo);
        map.put("remark", this.remark);
        map.put("createtime", sdf.format(this.createtime));
        map.put("completetime", this.completetime == null ? null : sdf.format(this.completetime));
        return map;
    }
}
