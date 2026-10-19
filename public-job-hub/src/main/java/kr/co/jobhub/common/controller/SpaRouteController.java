package kr.co.jobhub.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 새로고침해도 각 화면의 고유 URL에서 SPA 진입 문서를 반환한다. */
@Controller
public class SpaRouteController {
    @GetMapping({"/jobs", "/mypage", "/community", "/admin", "/postings/{id}"})
    public String appRoutes() { return "forward:/index.html"; }
}
