package com.leyou.goods.service;


import com.leyou.goods.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@Service
public class GoodsHtmlService {
    @Autowired
    private TemplateEngine engine;


    @Autowired
    private GoodsService goodsService;
    public void createHtml(Long spuId){
        //初始化运行上下文
        Context context = new Context();
        context.setVariables(this.goodsService.loadData(spuId));
        PrintWriter printWriter = null;
        try{
            File file = new File("/usr/local/etc/nginx/"+spuId+".html");
            printWriter = new PrintWriter(file);
            this.engine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if(printWriter!=null){
                printWriter.close();
            }
        }
    }

    public void asyncExcute(Long spuId) {
        ThreadUtils.execute(()->createHtml(spuId));
        /*ThreadUtils.execute(new Runnable() {
            @Override
            public void run() {
                createHtml(spuId);
            }
        });*/
    }

    public void deleteHtml(Long id) {
        File file = new File("/usr/local/etc/nginx/"+id+".html");
        file.delete();
    }
}
