package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Face
import com.icecreamqaq.yuq.message.FlashImage
import com.icecreamqaq.yuq.message.Image
import com.icecreamqaq.yuq.message.Voice
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.mirai.entity.ContactImpl
import com.icecreamqaq.yuq.mirai.entity.GroupImpl
import com.icecreamqaq.yuq.mirai.entity.GroupMemberImpl
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import net.mamoe.mirai.contact.Member as MiraiMember
import net.mamoe.mirai.message.data.At as MiraiAt
import net.mamoe.mirai.message.data.Face as MiraiFace
import net.mamoe.mirai.message.data.Image as MiraiImage
import net.mamoe.mirai.message.data.Voice as MiraiVoice


class TextImpl(override var text: String) : MessageItemBase(), Text {

    override fun toLocal(contact: Contact) = PlainText(text)

}

class AtImpl(override var user: Long) : MessageItemBase(), At {

    override fun toLocal(contact: Contact) =
        if (contact is GroupImpl)
            if (user == -1L) AtAll
            else MiraiAt(contact[user].miraiContact as MiraiMember)
        else PlainText("@$user")

}

class AtMemberImpl(override val member: Member) : MessageItemBase(), AtByMember {
    override fun toLocal(contact: Contact) = MiraiAt((member as GroupMemberImpl).miraiContact as MiraiMember)
}

class FaceImpl(override val faceId: Int) : MessageItemBase(), Face {

    override fun toLocal(contact: Contact) = MiraiFace(faceId)

}

class ImageSend(val ei: ExternalResource) : MessageItemBase(), Image {

    override lateinit var id: String
    override lateinit var url: String

    override fun toLocal(contact: Contact): Any {
        contact as ContactImpl
        val image = runBlocking { contact.miraiContact.uploadImage(ei) }
        id = image.imageId
        return image
    }

    override fun toPath() = "图片"

}

open class ImageReceive(override val id: String, override val url: String) : MessageItemBase(), Image {

    override fun toLocal(contact: Contact): Any {
        val image = MiraiImage(id)
        return image
//        val cType = contact is GroupImpl
//        val iType = image is GroupImage
//        return if (cType == iType) image else runBlocking { mif.imageByUrl(image.queryUrl()).toLocal(contact) }
    }

}

class FlashImageImpl(override val image: Image) : MessageItemBase(), FlashImage {

    override fun toLocal(contact: Contact): Any {
        return (image.toLocal(contact) as MiraiImage).flash()
    }

    override fun toPath() = "闪照"
}

class VoiceRecv(
    val miraiVoice: MiraiVoice
) : MessageItemBase(), Voice {

    override val id: String = miraiVoice.fileName
    override val url: String = miraiVoice.url ?: ""

    override fun toLocal(contact: Contact) = miraiVoice
}

class VoiceSend(val inputStream: InputStream) : MessageItemBase(), Voice {

    lateinit var miraiVoice: MiraiVoice

    override fun toPath() = if (::miraiVoice.isInitialized) miraiVoice.url ?: "" else "语音"

    override val id: String
        get() = miraiVoice.fileName
    override val url: String
        get() = miraiVoice.url ?: ""

    override fun toLocal(contact: Contact): Any {
        return if (::miraiVoice.isInitialized) miraiVoice
        else if (contact is GroupImpl)
            runBlocking {
                contact.group.uploadVoice(inputStream.toExternalResource())
            }.apply { miraiVoice = this }
        else error("mirai send voice only supposed group!")
    }

}

class XmlImpl(override val serviceId: Int, override val value: String) : MessageItemBase(), XmlEx {

    override fun toLocal(contact: Contact) = SimpleServiceMessage(serviceId, value)

}

class JsonImpl(override val value: String) : MessageItemBase(), JsonEx {

    override fun toLocal(contact: Contact) = LightApp(value)

}

class NoImplItemImpl(override var source: Any) : MessageItemBase(), NoImplItem {
    override fun toLocal(contact: Contact) = source
}

