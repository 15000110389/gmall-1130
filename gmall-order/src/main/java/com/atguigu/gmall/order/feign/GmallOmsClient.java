package com.atguigu.gmall.order.feign;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.api.GmallOmsApi;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@FeignClient("oms-service")
public interface GmallOmsClient extends GmallOmsApi {

}
