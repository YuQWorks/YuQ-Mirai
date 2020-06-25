package com.icecreamqaq.yuq.mirai.message

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.util.IO
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.message.*
import java.io.File
import javax.inject.Inject

class MiraiMessageItemFactory : MessageItemFactory ,ApplicationService {

    @Inject
    private lateinit var web: Web

    override fun text(text: String) = TextImpl(text)

    override fun at(qq: Long) = AtImpl(qq)

    override fun face(id: Int) = FaceImpl(id)


    override fun image(file: File): Image {
        val image = ImageSend()
        image.imageFile = file
        return image
    }

    override fun image(url: String): Image {
        val file = IO.tmpFile()
        web.download(url, file)
        return image(file)
    }

    override fun voice(file: File): Voice {
        TODO("Not yet implemented")
    }

    override fun xmlEx(serviceId: Int, value: String): XmlEx = XmlImpl(serviceId, value)

    override fun jsonEx(value: String) = JsonImpl(value)
    override fun init() {

    }

    override fun start() {
    }

    override fun stop() {
    }
}