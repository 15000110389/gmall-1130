package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.servcie.ItemServcie;
import com.atguigu.gmall.item.vo.ItemVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;

@Controller
public class ItemController {
    @Resource
    private ItemServcie itemServcie;

    @GetMapping("{skuId}.html")
    public String loadItem(@PathVariable("skuId")Long skuId, Model model){
        ItemVo item=itemServcie.loadItem(skuId);
        model.addAttribute("itemVo", item);
        return "item";
    }

}
