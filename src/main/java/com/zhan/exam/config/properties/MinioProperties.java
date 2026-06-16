package com.zhan.exam.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * projectName: ai_exam_system
 * className: MinioProperties
 * package: com.zhan.exam.config.properties
 * date: 2026/6/15 22:57
 *
 * @author: zhan
 * description:
 */

//@Component
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

}
