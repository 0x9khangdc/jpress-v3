package io.jpress.model.base;

import io.jboot.db.model.JbootModel;
import com.jfinal.plugin.activerecord.IBean;

/**
 * Generated by JPress, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseTemplateBlockOption<M extends BaseTemplateBlockOption<M>> extends JbootModel<M> implements IBean {

    private static final long serialVersionUID = 1L;

	public void setId(java.lang.Long id) {
		set("id", id);
	}

	public java.lang.Long getId() {
		return getLong("id");
	}

    /**
     * block_info 的主键ID
     */
	public void setBid(java.lang.Integer bid) {
		set("bid", bid);
	}

    /**
     * block_info 的主键ID
     */
	public java.lang.Integer getBid() {
		return getInt("bid");
	}

    /**
     * 配置的 key
     */
	public void setKey(java.lang.String key) {
		set("key", key);
	}

    /**
     * 配置的 key
     */
	public java.lang.String getKey() {
		return getStr("key");
	}

    /**
     * 配置的内容
     */
	public void setValue(java.lang.String value) {
		set("value", value);
	}

    /**
     * 配置的内容
     */
	public java.lang.String getValue() {
		return getStr("value");
	}

}
