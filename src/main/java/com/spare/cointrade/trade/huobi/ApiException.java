package com.spare.cointrade.trade.huobi;

/**
 * ApiException if api returns error.
 * 
 * @author liaoxuefeng
 */
public class ApiException extends RuntimeException {

  final String errCode;

  public ApiException(String errCode, String errMsg) {
    super(errMsg);
    this.errCode = errCode;
  }

  public ApiException(Exception e) {
    super(e);
    this.errCode = e.getClass().getName();
  }

  public String getErrCode() {
    return this.errCode;
  }

}
