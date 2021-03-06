/* 
 * IndustryGroupDAOImpl.java  
 * 
 * version TODO
 *
 * 2016年9月28日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.member.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.zlebank.zplatform.commons.dao.impl.HibernateBaseDAOImpl;
import com.zlebank.zplatform.commons.utils.StringUtil;
import com.zlebank.zplatform.member.bean.IndustryGroupBean;
import com.zlebank.zplatform.member.bean.IndustryGroupQuery;
import com.zlebank.zplatform.member.dao.IndustryGroupDAO;
import com.zlebank.zplatform.member.pojo.PojoIndustryGroup;

/**
 * Class Description
 *
 * @author houyong
 * @version
 * @date 2016年9月28日 上午9:32:44
 * @since 
 */
@Repository
public class IndustryGroupDAOImpl extends HibernateBaseDAOImpl<PojoIndustryGroup> implements IndustryGroupDAO {
    
    private Criteria builedCri(IndustryGroupQuery queryBean){
        Criteria criteria=getSession().createCriteria(PojoIndustryGroup.class);
        if (queryBean.getId()>0) {
            criteria.add(Restrictions.eq("id", queryBean.getId()));
        }
        if (StringUtil.isNotEmpty(queryBean.getGroupCode())) {
            criteria.add(Restrictions.eq("groupCode", queryBean.getGroupCode()));
        }
        if (StringUtil.isNotEmpty(queryBean.getGroupName())) {
            criteria.add(Restrictions.eq("groupName", queryBean.getGroupName()));
        }
        if (StringUtil.isNotEmpty(queryBean.getMemberId())) {
            criteria.add(Restrictions.eq("memberId", queryBean.getMemberId()));
        }
        if (StringUtil.isNotEmpty(queryBean.getInstiCode())) {
            criteria.add(Restrictions.eq("instiCode", queryBean.getInstiCode()));
        }
        if (queryBean.getStatus()!=null) {
            criteria.add(Restrictions.eq("status", queryBean.getStatus()));
        }
        return criteria;
    }
    /**
     *
     * @param groupBean
     * @return
     */
    @Override
    public PojoIndustryGroup getByCode(IndustryGroupBean groupBean) {
        Criteria criteria=getSession().createCriteria(PojoIndustryGroup.class);
        criteria.add(Restrictions.eq("groupCode", groupBean.getGroupCode()));
        return (PojoIndustryGroup) criteria.uniqueResult();
    }
    
    /**
     *
     * @param queryBean
     * @return
     */
    @Override
    public PojoIndustryGroup queryGroup(IndustryGroupQuery queryBean) {
        Criteria cri=builedCri(queryBean);
        if (cri.uniqueResult()!=null) {
            return (PojoIndustryGroup) cri.uniqueResult();
        }
        return null;
    }
    /**
     *
     * @param example
     * @return
     */
    @Override
    public long count(IndustryGroupQuery queryBean) {
        Criteria cri=builedCri(queryBean);
        
        return cri.list().size();
    }
    /**
     *
     * @param offset
     * @param pageSize
     * @param queryBean
     * @return
     */
    @Override
    public List<PojoIndustryGroup> getItem(int offset,
            int pageSize,
            IndustryGroupQuery queryBean) {
        Criteria criteria=builedCri(queryBean);
        criteria.setFirstResult(offset).setMaxResults(pageSize);
        return criteria.list();
    }

   

}
