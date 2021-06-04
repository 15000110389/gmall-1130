package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {
    @Resource
    private CartService cartService;
//    @Resource
//    private LoginInterceptor loginInterceptor;
    @GetMapping("test")
    @ResponseBody
    public String test() {
//        System.out.println(loginInterceptor);
        LoginInterceptor.getUserInfo();
        return "hello test";
    }

    @GetMapping()
    public String addCart(Cart cart){
        cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId="+cart.getSkuId()+"&count="+cart.getCount();
    }
    @GetMapping("addCart.html")
    public String queryCart(@RequestParam("skuId")Long skuId, @RequestParam("count")Integer count, Model model) {
        Cart cart=cartService.queryCart(skuId);
        cart.setCount(new BigDecimal(count));
        model.addAttribute("cart",cart);
        return "addCart";
    }
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> cartList=cartService.queryCarts();
        model.addAttribute("carts",cartList);
        return "cart";
    }
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo undateNum(@RequestBody Cart cart){
        cartService.undateNum(cart);
        return ResponseVo.ok();
    }
    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo undateStatus(@RequestBody Cart cart){
        cartService.undateStatus(cart);
        return ResponseVo.ok();
    }
    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam Long skuId) {
        cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

}
