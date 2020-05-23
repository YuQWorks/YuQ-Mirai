package com.icecreamqaq.yuq.mirai.message

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.PermissionDeniedException

class MiraiMessage : Message() {

    override fun plus(text: String): Message {
        body.add(TextImpl(text))
        return this
    }

    override fun newMessage(): Message {
        val message = MiraiMessage()
        message.temp = this.temp
        message.qq = this.qq
        message.group = this.group
        return message
    }
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