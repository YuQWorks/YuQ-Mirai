package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.annotation.AutoBind
import java.io.File

@AutoBind
interface MessageItemFactory {

    fun text(text:String):Text
    fun at(qq:Long):At
    fun face(id:Int):Face
    fun image(file: File):Image
    fun image(url:String):Image

}