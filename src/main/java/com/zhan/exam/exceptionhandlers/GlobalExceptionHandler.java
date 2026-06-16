package com.zhan.exam.exceptionhandlers;

import com.zhan.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * projectName: ai_exam_system
 * className: GlobalExceptionHandler
 * package: com.zhan.exam.exceptionhandlers
 * date: 2026/6/16 11:58
 *
 * @author: zhan
 * description:
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //定义异常处理的handler
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception e){
        //打印日志
        e.printStackTrace();//错误的堆栈信息先打印
        log.error("代码出现异常，异常信息为：{}",e.getMessage());
        return  Result.error(e.getMessage());
    }
}
