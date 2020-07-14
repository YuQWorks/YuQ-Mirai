package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.message.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.AtAll
import net.mamoe.mirai.message.data.LightApp
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.ServiceMessage
import net.mamoe.mirai.utils.toExternalImage
import java.io.File

abstract class MiraiMessageItemBase : MessageItem {
    override operator fun plus(item: MessageItem): Message = toMessage() + item
    override operator fun plus(item: String): Message = toMessage() + item
    override operator fun plus(item: Message): Message = toMessage() + item
    override fun toMessage(): Message = MiraiMessage() + this
    override fun toString() = toPath()
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

    override fun toString() = "\"" + text.replace("\n", "\\n") + "\""

    override fun equals(other: Any?): Boolean {
        if (other !is Text) return false
        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}

class AtImpl(override var user: Long) : MiraiMessageItemBase(), At {

    override fun toLocal(source: Any, message: Message) =
            when {
                source !is Bot -> throw RuntimeException("Not Allow Invoke")
                user == -1L -> AtAll
                else -> net.mamoe.mirai.message.data.At(source.groups[message.group!!][user])
            }

    override fun toPath() = "At_$user"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> user.toString()
        PathVar.Type.Long -> user
        PathVar.Type.Double -> user.toDouble()
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (other !is At) return false
        return user == other.user
    }

    override fun hashCode(): Int {
        return user.hashCode()
    }
}

class FaceImpl(override val faceId: Int) : MiraiMessageItemBase(), Face {

    override fun toLocal(source: Any, message: Message) = net.mamoe.mirai.message.data.Face(faceId)
    override fun toPath() = "表情_$faceId"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> "表情_$faceId"
        PathVar.Type.Integer -> faceId
        PathVar.Type.Long -> faceId.toLong()
        PathVar.Type.Double -> faceId.toDouble()
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Face) return false
        return faceId == other.faceId
    }

    override fun hashCode(): Int {
        return faceId
    }
}

class ImageSend : MiraiMessageItemBase(), Image {

    override lateinit var id: String
    override lateinit var url: String
    lateinit var imageFile: File

    override fun toLocal(source: Any, message: Message): Any {
        if (source !is Bot) throw RuntimeException("Not Allow Invoke")
        val externalImage = imageFile.toExternalImage()
        val image = runBlocking {
            if (message.group == null) source.friends[message.qq!!].uploadImage(externalImage)
            else source.groups[message.group!!].uploadImage(externalImage)
        }
        id = image.imageId
        return image
    }

    override fun toPath() = "图片"
    override fun convertByPathVar(type: PathVar.Type) = null

    override fun equals(other: Any?): Boolean {
        if (other !is Image) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + imageFile.hashCode()
        return result
    }
}

class XmlImpl(override val serviceId: Int, override val value: String) : MiraiMessageItemBase(), XmlEx {

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> value
        PathVar.Type.Source -> this
        else -> null
    }

    override fun toLocal(source: Any, message: Message) = ServiceMessage(serviceId, value)

    override fun toPath() = "XmlMsg"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XmlImpl

        if (serviceId != other.serviceId) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceId
        result = 31 * result + value.hashCode()
        return result
    }


}

class JsonImpl(override val value: String) : MiraiMessageItemBase(), JsonEx {

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> value
        PathVar.Type.Source -> this
        else -> null
    }

    override fun toLocal(source: Any, message: Message) = LightApp(value)

    override fun toPath() = "JsonMsg"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JsonImpl

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }


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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageReceive

        if (id != other.id) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }


}

class NoImplItemImpl(override var source: Any) : MiraiMessageItemBase(), NoImplItem {
    override fun toLocal(source: Any, message: Message) = source
    override fun toPath() = "NoImpl"
    override fun convertByPathVar(type: PathVar.Type) = null


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoImplItemImpl

        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        return source.hashCode()
    }
}