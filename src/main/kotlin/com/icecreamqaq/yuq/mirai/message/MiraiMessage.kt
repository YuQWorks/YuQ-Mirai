package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.bot
import net.mamoe.mirai.message.data.buildMessageChain

fun Message.toLocal(contact: Contact): MessageChain {
    var mm = buildMessageChain {}

    if (this.reply != null) mm += QuoteReply((this.reply as MiraiMessageSource).source)
    if (this.at != null) {
        mm += AtImpl(at!!.id).toLocal(contact)
        if (at!!.newLine) mm += "\n"
    }

    for (messageItem in this.body) {
        mm += messageItem.toLocal(contact) as net.mamoe.mirai.message.data.Message
    }

    return mm
}

class MiraiMessageSource(val source: net.mamoe.mirai.message.data.MessageSource) : MessageSource {
    override val id: Int
        get() = source.ids[0]
    override val liteMsg: String = source.contentToString()
    override val sendTime: Long
        get() = TODO("Not yet implemented")
    override val sender: Long
        get() = TODO("Not yet implemented")

    override fun recall(): Int {
        return runBlocking {
            return@runBlocking try {
                source.recall()
                0
            } catch (e: PermissionDeniedException) {
                -1
            } catch (e: IllegalStateException) {
                -2
            }
        }
    }
}