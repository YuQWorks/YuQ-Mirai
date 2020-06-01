package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface MessageFactory {

    fun newMessage():Message
    fun newGroup(group:Long):Message
    fun newPrivate(qq:Long):Message
    fun newTemp(group:Long,qq:Long):Message

}