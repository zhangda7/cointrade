package com.spare.cointrade.account;

import com.spare.cointrade.model.Account;

public interface IAccountService {

    /**
     * 获取指定平台的账户balance信息
     * 具体就是指该平台，每个币的数量是多少
     * @return
     */
    Account fetchBalance();

}
