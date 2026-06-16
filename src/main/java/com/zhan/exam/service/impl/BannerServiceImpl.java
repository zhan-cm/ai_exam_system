package com.zhan.exam.service.impl;

import com.zhan.exam.entity.Banner;
import com.zhan.exam.mapper.BannerMapper;
import com.zhan.exam.service.BannerService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhan.exam.service.FileUploadService;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public String uploadBannerImage(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //文件非空校验
        if (file.isEmpty()) {
            throw new RuntimeException("上传的轮播图文件为空，请重新上传");
        }
        //文件的类型 image ->mimetype image / png gif jpeg
        String contentType = file.getContentType();
        if(ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image")){
            throw new RuntimeException("上传的轮播图文件类型错误，请重新上传");
        }
        //调用文件上传业务
        String url = fileUploadService.uploadFile("banners",file);
        //回显地址
        return url;
    }
}