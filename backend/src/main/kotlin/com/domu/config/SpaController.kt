package com.domu.config

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class SpaController {

    @RequestMapping(value = ["/{path:^(?!api|files).*\$}", "/{path:^(?!api|files).*\$}/**"])
    fun forward(): String = "forward:/index.html"
}
