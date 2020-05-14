package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.router.MethodInvoker
import com.IceCreamQAQ.Yu.controller.ActionContext
import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.annotation.Save
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Named

class BotReflectMethodInvoker(val method: Method, val instance: Any) : MethodInvoker {

    var returnFlag: Boolean = false
    var mps: Array<MethodPara?>? = null
    lateinit var saves: Array<Saves>

    init {
        returnFlag = (method.returnType?.name ?: "void") != "void"

        val paras = method.parameters!!
        val mps = arrayOfNulls<MethodPara>(paras.size)

        val saves = ArrayList<Saves>(paras.size)
        for (i in paras.indices) {
            val para = paras[i]!!
            val name = para.getAnnotation(Named::class.java)!!.value
            val save = para.getAnnotation(Save::class.java)
            if (save != null) {
                saves.add(Saves(i, name))
            }

            val pathVar = para.getAnnotation(PathVar::class.java)
            if (pathVar != null) {
                mps[i] = MethodPara(para.type, 1, pathVar)
                continue
            }

            mps[i] = MethodPara(para.type, 0, name)
        }

        this.mps = mps
        this.saves = saves.toTypedArray()
    }

    override fun invoke(context: ActionContext): Any? {
        if (context !is BotActionContext) return null
        val mps = mps!!
        val paras = arrayOfNulls<Any>(mps.size)

        for (i in mps.indices) {
            val mp = mps[i] ?: continue
            paras[i] = when (mp.type) {
                0 -> context[mp.data as String]
                1 -> {
                    val pv = mp.data as PathVar
                    when {
                        context.message!!.body.size <= pv.value -> null
                        pv.type == PathVar.Type.Source -> context.message!!.body[pv.value]
                        else -> context.message!!.body[pv.value].convertByPathVar(pv.type)
                    }
                }
                else -> null
            }
        }

        try {

            val re = if (mps.isEmpty()) method.invoke(instance)
            else method.invoke(instance, *paras)

            for (save in saves) {
                context.session[save.name] = paras[save.i] ?: continue
            }

            if (returnFlag) return re
            return null

        } catch (e: InvocationTargetException) {
            throw e.cause!!
        }
    }

    data class MethodPara(
            val clazz: Class<*>,
            val type: Int,
            val data: Any
    )

    data class Saves(val i: Int, val name: String)
}