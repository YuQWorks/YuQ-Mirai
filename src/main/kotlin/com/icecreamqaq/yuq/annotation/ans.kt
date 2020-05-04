package com.icecreamqaq.yuq.annotation

import com.IceCreamQAQ.Yu.annotation.EnchantBy
import com.IceCreamQAQ.Yu.annotation.LoadBy_
import com.IceCreamQAQ.Yu.controller.DefaultControllerLoaderImpl
import com.IceCreamQAQ.Yu.loader.enchant.MethodParaNamedEnchanter
import com.icecreamqaq.yuq.controller.BotControllerLoader
import javax.inject.Named


@LoadBy_(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("group")
annotation class GroupController

@LoadBy_(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("priv")
annotation class PrivateController

annotation class PathVar(val value:Int,val type:Type){
    enum class Type{
        String,Integer,Switch,Long,Double
    }
}