package com.rhododendra.controller;

import com.rhododendra.model.Species;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class WebController {
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/test")
    public String test(Model model) {
        model.addAttribute("rhodos",
            List.of(
                new Species("TestRhodo1"),
                new Species("TestRhodo2"),
                new Species("TestRhodo3")
            )
        );
        return "test";
    }


}