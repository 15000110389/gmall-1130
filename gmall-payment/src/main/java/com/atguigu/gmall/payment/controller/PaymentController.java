package com.atguigu.gmall.payment.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.pojo.PayAsyncVo;
import com.atguigu.gmall.payment.pojo.PayVo;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.service.PaymentService;
import com.sun.javafx.collections.MappingChange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken")String orderToken, Model model){

        // 查询订单是否存在
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity == null){
            throw new OrderException("您要支付的订单不存在！");
        }

        // 如果存在判断是否自己的
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (orderEntity.getUserId() != userId){
            throw new OrderException("要支付的订单不属于您！");
        }

        // 判断订单状态
        if (orderEntity.getStatus() != 0){
            throw new OrderException("当前订单无法支付！");
        }

        model.addAttribute("orderEntity", orderEntity);

        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody
    public String toAlipay(@RequestParam()String orderToken) {
        // 查询订单是否存在
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity == null){
            throw new OrderException("您要支付的订单不存在！");
        }

        // 如果存在判断是否自己的
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (orderEntity.getUserId() != userId){
            throw new OrderException("要支付的订单不属于您！");
        }

        // 判断订单状态
        if (orderEntity.getStatus() != 0){
            throw new OrderException("当前订单无法支付！");
        }
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        payVo.setTotal_amount("0.01");
        payVo.setSubject("谷粒商城支付平台");
        Long payId = this.paymentService.savePaymentInfo(payVo);
        payVo.setPassback_params(payId.toString());
        String form=null;
        try {
            form = alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return null;
        }
        return form;
    }
    /**
     * 同步回调
     * @return
     */


    @GetMapping("pay/success")
    public String paySuccess(){

        // TODO: 获取订单编号，查询订单

        return "paysuccess";
    }

    /**
     * 异步回调
     * @return
     */
    @PostMapping("pay/ok")
    public Object payOk(PayAsyncVo payAsyncVo){
        // 1.验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验业务参数：app_id、out_trade_no、total_amount
        String app_id = this.alipayTemplate.getApp_id(); // 服务内的appId
        String appId = payAsyncVo.getApp_id();  // 支付宝响应的appId

        // 订单编号
        String out_trade_no = payAsyncVo.getOut_trade_no(); // 支付宝响应的订单编号
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryById(payId);
        String outTradeNo = paymentInfoEntity.getOutTradeNo(); // 获取对账表中的订单编号

        // 金额
        String total_amount = payAsyncVo.getTotal_amount();
        BigDecimal totalAmount = paymentInfoEntity.getTotalAmount();
        if (!StringUtils.equals(app_id, appId) || !StringUtils.equals(out_trade_no, outTradeNo)
                || new BigDecimal(total_amount).compareTo(totalAmount) != 0
        ){
            return "failure";
        }

        // 3.校验回调的状态：TRADE_SUCCESS
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())){
            return "failure";
        }

        // 4.记录对账信息
        if (this.paymentService.udpatePaymentInfo(payAsyncVo) == 1) {
            // 5.发送消息给mq，修改订单状态 pay -> oms -> wms
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.pay", out_trade_no);
        }
        // 返回成功
        return "success";
    }



}
