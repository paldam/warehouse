package com.damian.service;

import com.damian.model.DbFile;
import com.damian.repository.DbFileDao;
import com.damian.rest.OrderController;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

@Service
public class DbFileService {
    private static final Logger logger = Logger.getLogger(DbFileService.class);

    private DbFileDao dbFileDao;

    public DbFileService(DbFileDao dbFileDao) {
        this.dbFileDao=dbFileDao;
    }

     public void uploadFiles(MultipartFile[] files, Long orderId) {



         Arrays.asList(files).forEach(file -> {
             DbFile uploadedFile = new DbFile();
             uploadedFile.setFileName(file.getOriginalFilename());
             uploadedFile.setFileType(file.getContentType());
             uploadedFile.setOrderId(orderId);

             try{
                 uploadedFile.setData(file.getBytes());
             } catch (IOException ex) {
                 ex.printStackTrace();
             }
             dbFileDao.save(uploadedFile);
         });
         
     }

}
