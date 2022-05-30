package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Face
import com.icecreamqaq.yuq.message.FlashImage
import com.icecreamqaq.yuq.message.Image
import com.icecreamqaq.yuq.message.Voice
import com.icecreamqaq.yuq.mirai.entity.ContactImpl
import com.icecreamqaq.yuq.mirai.entity.GroupImpl
import com.icecreamqaq.yuq.mirai.entity.GroupMemberImpl
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.InputStream
import net.mamoe.mirai.contact.Member as MiraiMember
import net.mamoe.mirai.message.data.At as MiraiAt
import net.mamoe.mirai.message.data.Face as MiraiFace
import net.mamoe.mirai.message.data.Image as MiraiImage
import net.mamoe.mirai.message.data.Audio as MiraiVoice


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

class ImageSend(private val ei: ExternalResource) : MessageItemBase(), Image {

    override lateinit var id: String
    override lateinit var url: String

    lateinit var image: MiraiImage

    override fun toLocal(contact: Contact): Any {
        contact as ContactImpl
        if (::image.isInitialized) return image
        image = runBlocking { contact.miraiContact.uploadImage(ei) }
        ei.close()
        id = image.imageId
        return image
    }

    override fun toPath() = "图片"

}

open class ImageReceive(id: String, override val url: String) : MessageItemBase(), Image {

    override val id: String = if (id.startsWith("{")) id.replace("{", "").replace("}", "").replace("-", "") else id

    override fun toLocal(contact: Contact): Any {
        return MiraiImage(id.split(".").let { "{${it[0].toUUID()}}.${it[1]}" })
//        val cType = contact is GroupImpl
//        val iType = image is GroupImage
//        return if (cType == iType) image else runBlocking { mif.imageByUrl(image.queryUrl()).toLocal(contact) }
    }

    private fun String.toUUID(): String = "${this[0..7]}-${this[8..11]}-${this[12..15]}-${this[16..19]}-${this[20..31]}"

    private operator fun String.get(intRange: IntRange): String {
        val sb = StringBuilder()

        for (i in intRange) {
            sb.append(this[i])
        }
        return sb.toString()
    }

}


class FlashImageImpl(override val image: Image) : MessageItemBase(), FlashImage {

    override fun toLocal(contact: Contact): Any {
        return (image.toLocal(contact) as MiraiImage).flash()
    }

    override fun toPath() = "闪照"
}

class VoiceRecv(
    val miraiVoice: OnlineAudio
) : MessageItemBase(), Voice {

    override val id: String = miraiVoice.filename
    override val url: String = miraiVoice.urlForDownload ?: ""

    override fun toLocal(contact: Contact) = miraiVoice
}

class VoiceSend(val inputStream: InputStream) : MessageItemBase(), Voice {

    lateinit var miraiVoice: MiraiVoice

    override fun toPath() = if (::miraiVoice.isInitialized) miraiVoice.filename ?: "" else "语音"

    override val id: String
        get() = miraiVoice.filename
    override val url: String
        get() = ""

    override fun toLocal(contact: Contact): Any {
        return if (::miraiVoice.isInitialized) miraiVoice
        else if (contact is GroupImpl)
            runBlocking {
                contact.group.uploadAudio(inputStream.toExternalResource())
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

