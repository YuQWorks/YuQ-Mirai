package com.icecreamqaq.yuqui

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import tornadofx.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PhoneCap : View("手机验证码") {

    val waitC : Continuation<String> by param()
    val phoneCap = SimpleStringProperty()

    override val root = form {
        label("请输入手机验证码：")
        fieldset {
            field("手机验证码：") {
                textfield(phoneCap)
            }
            field {
                button("提交") {
                    action {
                        waitC.resume(phoneCap.value)
                        close()
                    }
                }
            }
        }
    }
}