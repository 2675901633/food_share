package cn.kmbeast.pojo.api;

/**
 * 响应基类
 *
 * @param <T>
 */
public class Result<T> {
    /**
     * 响应状态码
     */
    private Integer code;
    /**
     * 响应消息
     */
    private String msg;
    /**
     * 响应数据
     */
    private T data;

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 判断请求是否成功
     * 
     * @return 如果状态码为200则返回true，否则返回false
     */
    public boolean isSuccess() {
        return code != null && code.equals(ResultCode.REQUEST_SUCCESS.getCode());
    }

    public Result() {
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
