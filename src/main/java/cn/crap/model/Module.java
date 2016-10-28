package cn.crap.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import cn.crap.enumeration.ModuleStatus;
import cn.crap.framework.SpringContextHolder;
import cn.crap.framework.base.BaseModel;
import cn.crap.inter.service.tool.ICacheService;
import cn.crap.service.tool.CacheService;


@Entity
@Table(name="module")
@GenericGenerator(name="Generator", strategy="cn.crap.framework.IdGenerator")
public class Module extends BaseModel implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String password;
	private String url;
	private byte canDelete;
	private String remark;
	private String userId;
	private String projectId;
	

	public Module(){};
	public Module(String name) {
		this.name = name;
		this.userId = "superAdmin";
	}

	@Column(name="password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column(name="name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	@Column(name="url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(name="canDelete")
	public byte getCanDelete() {
		return canDelete;
	}
	public void setCanDelete(byte canDelete) {
		this.canDelete = canDelete;
	}
	
	@Column(name="remark")
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	
	@Column(name="userId")
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Transient
	public String getStatusName(){
		return ModuleStatus.getNameByValue(status+"");
	}
	
	
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	
	@Column(name="projectId")
	public String getProjectId(){
		return this.projectId;
	}
	
	@Transient
	public String getProjectName(){
		ICacheService cacheService = SpringContextHolder.getBean("cacheService", CacheService.class);
		return cacheService.getProject(projectId).getName();
	}
}
	