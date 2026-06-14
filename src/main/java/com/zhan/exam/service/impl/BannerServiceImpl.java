package com.zhan.exam.service.impl;

import com.zhan.exam.entity.Banner;
import com.zhan.exam.mapper.BannerMapper;
import com.zhan.exam.service.BannerService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

} 