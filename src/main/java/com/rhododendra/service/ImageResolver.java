package com.rhododendra.service;

import java.util.List;

public class ImageResolver {
    public static String resolveImagePath(String img){
        if (img == null) return null;
//        return "/img/" + img;
        return "https://d2rugg7g9y8njm.cloudfront.net/img/" + img;
    }

    public static List<String>  resolveImages(List<String> img){
        return img.stream().map(ImageResolver::resolveImagePath).toList();
    }
}
