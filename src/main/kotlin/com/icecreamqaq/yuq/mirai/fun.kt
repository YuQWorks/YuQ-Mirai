package com.icecreamqaq.yuq.mirai

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.yuq

fun Web.getWithQQKey(url: String): String {
    var u = url.replace("{gtk}", (yuq as MiraiBot).gtk.toString(), true)
    u = u.replace("{skey}", (yuq as MiraiBot).sKey, true)
    if (u.contains("{psgtk}", true)){
        val domain = u.split("://")[1].split("/")[0]
        for ((k, v) in (yuq as MiraiBot).pskeyMap) {
            if (domain.endsWith(k)){
                u = u.replace("{psgtk}",v.gtk.toString(),true)
                break
            }
        }
    }
    return this.get(u)
}