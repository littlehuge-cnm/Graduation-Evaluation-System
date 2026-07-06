package com.example.graduationevaluationsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.graduationevaluationsystem.entity.Document;
import com.example.graduationevaluationsystem.mapper.DocumentMapper;
import com.example.graduationevaluationsystem.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * 文档 Service 实现
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {
}
