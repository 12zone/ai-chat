package org.example.qasystem.model;


import lombok.Data;

@Data
public class ApiResponse <T>{
    
    private int retCode;
    private String retMsg;
    private T data;
    
    public ApiResponse(int retCode, String retMsg, T data) {
        this.retCode = retCode;
        this.retMsg = retMsg;
        this.data = data;
    }

    //success
    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<T>(200, "success", data);
    }
    public static <T> ApiResponse<T> success(){
        return new ApiResponse<T>(200, "success", null);
    }

    //fail
    public static <T> ApiResponse<T> errro(int retCode, String retMsg){
        return new ApiResponse<T>(retCode, retMsg, null);
    }
}
