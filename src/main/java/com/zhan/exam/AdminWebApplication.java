package com.zhan.exam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * projectName: ai_exam_system
 * className: AdminWebApplication
 * package: com.zhan.exam
 * date: 2026/6/14 0:40
 *
 * @author: zhan
 * description:
 */
@SpringBootApplication
public class AdminWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminWebApplication.class, args);
        System.out.println("项目启动成功~");
    }
}
