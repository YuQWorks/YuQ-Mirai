package com.icecreamqaq.yuq.mirai.message

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.util.IO
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.web
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class MiraiMessageItemFactory : MessageItemFactory {

    override fun text(text: String) = TextImpl(text)

    override fun at(member: Member) = AtMemberImpl(member)

    override fun at(qq: Long) = AtImpl(qq)

    override fun face(id: Int) = FaceImpl(id)


    override fun image(file: File) = imageByFile(file)

    override fun image(url: String) = imageByUrl(url)

    override fun imageByBufferedImage(bufferedImage: BufferedImage) = TODO()//ImageSend(bufferedImage.data.)

    override fun imageByFile(file: File) = ImageSend(file.toExternalResource())

    override fun imageById(id: String) = ImageReceive(id, "")

    override fun imageByInputStream(inputStream: InputStream) = ImageSend(inputStream.toExternalResource().apply { inputStream.close() })

    override fun imageByUrl(url: String) = imageByInputStream(web.download(url))

    override fun imageToFlash(image: Image) = FlashImageImpl(image)

    override fun voiceByInputStream(inputStream: InputStream) = VoiceSend(inputStream)

    override fun xmlEx(serviceId: Int, value: String): XmlEx = XmlImpl(serviceId, value)

    override fun jsonEx(value: String) = JsonImpl(value)
    override fun messagePackage(flag: Int, body: MutableList<IMessageItemChain>): MessagePackage {
        TODO("Not yet implemented")
    }
}