package com.zhan.exam.service.impl;

import com.zhan.exam.config.properties.MinioProperties;
import com.zhan.exam.service.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * projectName: com.zhan.exam.service.impl
 *
 * @author: 赵伟风
 * description:
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String uploadFile(String folder, MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //1.链接minio的客户端
        //2.判断桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
        //3.不存在创建桶，并设置访问权限
        if (!bucketExists) {
            String config = """
                        {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                        }
                    """.formatted(minioProperties.getBucketName());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(config)
                    .build());
        }
        //4.上传文件
        //希望在minio的服务器中，创建一个文件夹，再存储对象
        //banners/zhan.png --> minio banners -> zhan.png
        //1.位置防止重复，uuid进行对象名字的去重
        //2.日期 banners/20260616/uuid_文件名
        String objectName = folder + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" +
                UUID.randomUUID().toString().replaceAll("-","")+"_"+ file.getOriginalFilename();

        log.debug("文件上传核心业务方法，处理后的文件对象名：{}",objectName);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .contentType(file.getContentType())
                .object(file.getOriginalFilename())
                //stream上传文件的输入流数据
                //参数1： 就是要上传文件的输入流
                //参数2：上传文件的字节大小
                //参数3： 是否切割文件，转化多线程上传 -1表示交给minio智能切割
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());
        //5.拼接回显地址
        //url = 端点 + 桶 + 对象名
        String url = String.join("/",minioProperties.getEndpoint(),minioProperties.getBucketName(),objectName);
        log.info("文件上传成功，回显地址为：{}",url);
        return url;
    }
}
