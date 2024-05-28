package com.yang.log.controller;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {


    @GetMapping
    public TestVo test(TestVo testVo){
        return new TestVo().setName(Arrays.asList("z","w"));
    }

    @PostMapping
    public TestVo test(String name,@RequestBody TestVo testVo){
        return new TestVo().setName(Arrays.asList("z","w"));
    }

    @GetMapping("/upload")
    public String upload(){
        log.warn("zzzz");
        return "upload";
    }

    @Data
    @Accessors(chain = true)
    public static class TestVo{
        List<String> name;
    }
}
