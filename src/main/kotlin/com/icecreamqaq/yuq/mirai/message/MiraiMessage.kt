package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mirai.MiraiBot
import com.icecreamqaq.yuq.yuq
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain

fun Message.toLocal(contact: Contact): MessageChain {
    var mm = buildMessageChain {}

    if (this.reply != null) mm += QuoteReply((this.reply as MiraiMessageSource).source)
    if (this.at) mm += AtImpl(this.qq!!).toLocal(contact)

    for (messageItem in this.body) {
        mm += messageItem.toLocal(contact) as net.mamoe.mirai.message.data.Message
    }

    return mm
}

class MiraiMessageSource(val source: net.mamoe.mirai.message.data.MessageSource) : MessageSource {
    override val id: Int
        get() = source.id

    override fun recall(): Int {
        return runBlocking {
            return@runBlocking try {
                source.bot.recall(source)
                0
            } catch (e: PermissionDeniedException) {
                -1
            } catch (e: IllegalStateException) {
                -2
            }
        }
    }
}