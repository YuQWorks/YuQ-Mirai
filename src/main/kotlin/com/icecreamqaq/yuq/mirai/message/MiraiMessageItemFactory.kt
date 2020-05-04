package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.*
import java.io.File

class MiraiMessageItemFactory : MessageItemFactory {
    override fun text(text: String): Text {
        return TextImpl(text)
    }

    override fun at(qq: Long): At {
        return AtImpl(qq)
    }

    override fun face(id: Int): Face {
        return FaceImpl(id)
    }

    override fun image(file: File): Image {
        val image = ImageSend()
        image.imageFile = file
        return image
    }

    override fun image(url: String): Image {
        TODO("Not yet implemented")
    }
}