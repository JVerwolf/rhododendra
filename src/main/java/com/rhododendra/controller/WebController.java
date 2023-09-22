package com.rhododendra.controller;

import org.springframework.stereotype.Controller;

@Controller
public class WebController {
    public String index() {
        return "index";
    }

//    @RequestMapping(value = "/test")
//    public String test(Model model) {
//        model.addAttribute("rhodos",
//            List.of(
//                new Species("TestRhodo1"),
//                new Species("TestRhodo2"),
//                new Species("TestRhodo3")
//            )
//        );
//        return "test";
//    }


}