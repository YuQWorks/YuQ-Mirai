package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.*
import java.io.File

class MiraiMessageItemFactory : MessageItemFactory {

    override fun text(text: String) = TextImpl(text)

    override fun at(qq: Long) = AtImpl(qq)

    override fun face(id: Int) = FaceImpl(id)


    override fun image(file: File): Image {
        val image = ImageSend()
        image.imageFile = file
        return image
    }

    override fun image(url: String): Image {
        TODO("Not yet implemented")
    }

    override fun voice(file: File): Voice {
        TODO("Not yet implemented")
    }

    override fun xmlEx(serviceId: Int, value: String): XmlEx = XmlImpl(serviceId, value)

    override fun jsonEx(value: String) = JsonImpl(value)
}