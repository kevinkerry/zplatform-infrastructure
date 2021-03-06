/* 
 * BusiAcctServiceImpl.java  
 * 
 * version 1.0
 *
 * 2015年8月31日 
 * 
 * Copyright (c) 2015,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.acc.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zlebank.zplatform.acc.bean.Account;
import com.zlebank.zplatform.acc.bean.BusiAcct;
import com.zlebank.zplatform.acc.bean.QueryBusiCodeInfo;
import com.zlebank.zplatform.acc.bean.Subject;
import com.zlebank.zplatform.acc.bean.enums.BuisAcctCodePrefix;
import com.zlebank.zplatform.acc.bean.enums.Usage;
import com.zlebank.zplatform.acc.dao.AccountDAO;
import com.zlebank.zplatform.acc.dao.BusiAcctDAO;
import com.zlebank.zplatform.acc.exception.AbstractAccException;
import com.zlebank.zplatform.acc.exception.AbstractBusiAcctException;
import com.zlebank.zplatform.acc.exception.BusiAcctNotExistException;
import com.zlebank.zplatform.acc.exception.BusiAcctRepeatException;
import com.zlebank.zplatform.acc.pojo.PojoAccount;
import com.zlebank.zplatform.acc.pojo.PojoBusiAcct;
import com.zlebank.zplatform.acc.service.AccountService;
import com.zlebank.zplatform.acc.service.BusiAcctService;
import com.zlebank.zplatform.acc.service.SubjectSelector;
import com.zlebank.zplatform.commons.utils.BeanCopyUtil;
import com.zlebank.zplatform.member.bean.BusinessActor;

/**
 * Class Description
 *
 * @author yangying
 * @version
 * @date 2015年8月31日 下午1:59:40
 * @since
 */
@Service
public class BusiAcctServiceImpl implements BusiAcctService {
    private final Log log = LogFactory.getLog(BusiAcctServiceImpl.class);

    @Autowired
    private SubjectSelector mappingTableSubjectSelector;
    @Autowired
    private BusiAcctDAO busiAcctDAO;
    @Autowired
    private AccountService accountServiceImpl;
    @Autowired
    private AccountDAO accountDAO;
 

    /**
     * 根据业务参与者，用途开通一个业务账户和会计账户
     *
     * @param businessActor
     * @param busiAcct
     * @param userId
     * @return busiAcctCode
     * @throws AbstractBusiAcctException
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    @Override
    public String openBusiAcct(BusinessActor businessActor, BusiAcct busiAcct, long userId)
            throws AbstractBusiAcctException {
        // 选取父级科目
        Subject parentSubject = mappingTableSubjectSelector.select(businessActor, busiAcct);
        // 生成业务代码
        String busiAcctCode = gengrateBusiAcctCode(businessActor, busiAcct,
                parentSubject.getAcctCode(), "");
        busiAcct.setBusiAcctCode(busiAcctCode);

        // 新建业务账户
        PojoBusiAcct pojoBusiAcct = busiAcctDAO.getByBusiAcctCode(busiAcctCode);
        if (pojoBusiAcct != null) {
            // 已经存在的话抛出异常
            throw new BusiAcctRepeatException();
        }
        
        // 初始化会计账户
        Account account = new Account();
        account.setParentSubject(parentSubject);
        account.setAcctCodeName(busiAcct.getBusiAcctName());
        try {
            // 开通会计账户
            account = accountServiceImpl.openAcct(account, businessActor, userId);
        } catch (AbstractAccException e) {
            log.warn("Add account failed.Caused By: " + e.getMessage());
            throw new BusiAcctRepeatException(e);
        }

        // 保存业务账户
        pojoBusiAcct = saveBusiAcct(busiAcct, businessActor);

        // 绑定会计科目到账户科目
        pojoBusiAcct.setAccountId(account.getId());

        return pojoBusiAcct.getBusiAcctCode();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public BusiAcct getByBusiAcctCode(String busiAcctCode)
            throws AbstractBusiAcctException {
        PojoBusiAcct pojoBusiAcct = busiAcctDAO.getByBusiAcctCode(busiAcctCode);
        return BeanCopyUtil.copyBean(BusiAcct.class, pojoBusiAcct);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String getAccount(Usage usage, String memberId)
            throws AbstractBusiAcctException {
        if (log.isDebugEnabled()) {
            log.debug("通过会员ID和用途得到科目代码："+"【用途："+usage.getCode()+"】【会员号"+memberId+"】");
        }
        long accountId = busiAcctDAO.getAccount(usage, memberId);
        PojoAccount account = accountDAO.get(accountId);
        if (account == null) {
            throw new BusiAcctNotExistException();
        }
        return account.getAcctCode();
    }
    
    /**
     * <p>
     * businessActorTypePrefix(2)+usage(3)+businessActorType(2)+businessActorId(<8,15>).</p>
     * <p>For example,suppose a enterprise member (memberId:100000000000100). The busiAcctCode of the basic fund busiAccount of the member is:
     * 9010102100000000000100</p>
     * @param member
     * @param busiAcct
     * @param parentSubjectCode
     * @param productNo
     * @return
     */
    private String gengrateBusiAcctCode(BusinessActor businessActor,
            BusiAcct busiAcct,
            String parentSubjectCode,
            String productNo) {
        StringBuilder sb = new StringBuilder();
        switch (businessActor.getBusinessActorType()) {
            case INDIVIDUAL :
                sb.append(BuisAcctCodePrefix.PRIVATE.getCode());
                break;
            case ENTERPRISE :
                sb.append(BuisAcctCodePrefix.PUBLIC.getCode());
                break;
            case COOPINSTI :
                sb.append(BuisAcctCodePrefix.PUBLIC.getCode());
                break;
            case CHANNEL :
                sb.append(BuisAcctCodePrefix.PUBLIC.getCode());
                break;
            case PRODUCT :
                sb.append(BuisAcctCodePrefix.PRODUCT.getCode());
                break;
            default :
                //TODO throw new exception 
                break;
        }
         
        sb.append(busiAcct.getUsage().getCode());
        sb.append(businessActor.getBusinessActorType().getCode());
        sb.append(businessActor.getBusinessActorId());

        return sb.toString();
    }
    private PojoBusiAcct saveBusiAcct(BusiAcct busiAcct, BusinessActor member) {
        PojoBusiAcct pojoBusiAcct = new PojoBusiAcct();
        pojoBusiAcct.setUsage(busiAcct.getUsage());
        pojoBusiAcct.setBusiAcctCode(busiAcct.getBusiAcctCode());
        pojoBusiAcct.setBusiAcctName(busiAcct.getBusiAcctName());
        pojoBusiAcct.setBusinessActorId(member.getBusinessActorId());
        pojoBusiAcct.setUsage(busiAcct.getUsage());
        return busiAcctDAO.merge(pojoBusiAcct);
    }

    /**
     *  通过 会员ID 标示得到科目代码的ID
     * @param usage
     * @param memberId
     * @return
     * @throws AbstractBusiAcctException
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public long getAccountId(Usage usage, String memberId)  throws AbstractBusiAcctException {
        if (log.isDebugEnabled()) {
            log.debug("通过会员ID和用途得到科目代码："+"【用途："+usage.getCode()+"】【会员号"+memberId+"】");
        }
        long accountId = busiAcctDAO.getAccount(usage, memberId);
        return accountId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public QueryBusiCodeInfo getBusiCodeByMemberId(Usage usage, String memberId)
            throws AbstractBusiAcctException {
        // 取业务账户信息
        PojoBusiAcct pojoBusiAcct = busiAcctDAO.getBusiCode(usage, memberId);
        // 组成返回结果bean
        QueryBusiCodeInfo info = new QueryBusiCodeInfo();
        info.setAcctId(pojoBusiAcct.getAccountId());
        info.setBusiCode(pojoBusiAcct.getBusiAcctCode());
        return info;
    }
}
