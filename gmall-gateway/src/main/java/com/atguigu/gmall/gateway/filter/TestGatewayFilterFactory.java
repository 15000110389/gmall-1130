//package com.atguigu.gmall.gateway.filter;
//
//import lombok.Data;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//public class TestGatewayFilterFactory extends AbstractGatewayFilterFactory<TestGatewayFilterFactory.keyValueConfig> {
//    public TestGatewayFilterFactory() {
//        super(keyValueConfig.class);
//    }
//
//    @Override
//    public List<String> shortcutFieldOrder() {
//        return Arrays.asList("value","key");
//    }
//
//    @Override
//    public ShortcutType shortcutType() {
//        return ShortcutType.GATHER_LIST;
//    }
//
//    @Override
//    public GatewayFilter apply(keyValueConfig config) {
//        return new GatewayFilter() {
//            @Override
//            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                System.out.println("局部过滤器,key="+config.key+"value"+config.value);
//                return chain.filter(exchange);
//            }
//        };
//    }
//    @Data
//    public static final class keyValueConfig{
//        private String key;
//        private String value;
//
//    }
//}
