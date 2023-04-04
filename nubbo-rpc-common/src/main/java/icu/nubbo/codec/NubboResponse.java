package icu.nubbo.codec;

import java.io.Serializable;

/**
 * rpc响应
 * 包含状态码，响应信息，错误信息
 * 对应的请求id，返回结果
 * */
public class NubboResponse implements Serializable {
    private Integer code;

    private String msg;

    private String error;

    private String requestId;

    private Object result;

    public boolean isSuccess() {
        return error == null;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
