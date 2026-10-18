package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all information-related effect executors.
 */
class InformationExecutors(private val cardRegistry: CardRegistry) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        LookAtTargetHandExecutor(),
        LookAtFaceDownExecutor(),
        RevealHandEffectExecutor(),
        RevealCardInHandExecutor(cardRegistry)
    )
}
