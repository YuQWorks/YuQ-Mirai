package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.message.*
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.OfflineFriendImage
import net.mamoe.mirai.message.data.OnlineFriendImage
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.toExternalImage
import java.io.File
import java.lang.RuntimeException

abstract class MiraiMessageItemBase : MessageItem {
    override operator fun plus(item: MessageItem): Message = toMessage() + item
    override operator fun plus(item: String): Message = toMessage() + item
    override operator fun plus(item: Message): Message = toMessage() + item
    override fun toMessage(): Message = MiraiMessage() + this
}

class TextImpl(override var text: String) : MiraiMessageItemBase(), Text {

    override fun toLocal(source: Any, message: Message) = PlainText(text)
    override fun toPath() = text
    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.Source -> this
        PathVar.Type.String -> text
        PathVar.Type.Switch -> {
            val textLow = text.toLowerCase()
            textLow.contains("true") || text.contains("开") || text.contains("启")
                    || textLow.contains("open") || textLow.contains("enable")
                    || textLow.contains("on") || text.contains("是")
        }
        PathVar.Type.Integer -> text.toInt()
        PathVar.Type.Long -> text.toLong()
        PathVar.Type.Double -> text.toDouble()
        else -> null
    }
}

class AtImpl(override var user: Long) : MiraiMessageItemBase(), At {

    override fun toLocal(source: Any, message: Message) =
            if (source !is Bot) throw RuntimeException("Not Allow Invoke")
            else net.mamoe.mirai.message.data.At(source.groups[message.group!!][user])

    override fun toPath() = "At_$user"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> user.toString()
        PathVar.Type.Long -> user
        PathVar.Type.Double -> user.toDouble()
        else -> null
    }

}

class FaceImpl(override val faceId: Int) : MiraiMessageItemBase(), Face {

    override fun toLocal(source: Any, message: Message) = net.mamoe.mirai.message.data.Face(faceId)
    override fun toPath() = "表情"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> "表情"
        PathVar.Type.Integer -> faceId
        PathVar.Type.Long -> faceId.toLong()
        PathVar.Type.Double -> faceId.toDouble()
        else -> null
    }

}

class ImageSend : MiraiMessageItemBase(), Image {

    override lateinit var id: String
    override lateinit var url: String
    lateinit var imageFile: File

    override fun toLocal(source: Any, message: Message): Any {
        if (source !is Bot) throw RuntimeException("Not Allow Invoke")
        val image = imageFile.toExternalImage()
        return runBlocking {
            if (message.group == null) source.friends[message.qq!!].uploadImage(image)
            else source.groups[message.group!!].uploadImage(image)
        }
    }

    override fun toPath() = "图片"
    override fun convertByPathVar(type: PathVar.Type) = null

}

class ImageReceive(override val id: String, override val url: String) : MiraiMessageItemBase(), Image {

    override fun toLocal(source: Any, message: Message) = net.mamoe.mirai.message.data.Image(id)

    override fun toPath(): String {
        return "img_$id"
    }

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> "图片"
        PathVar.Type.Source -> this
        else -> null
    }


}

class NoImplItemImpl(override var source: String) : MiraiMessageItemBase(), NoImplItem {
    override fun toLocal(source: Any, message: Message) = source
    override fun toPath() = "NoImpl"
    override fun convertByPathVar(type: PathVar.Type) = null
}