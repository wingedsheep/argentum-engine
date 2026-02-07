package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all information-related effect executors.
 */
class InformationExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        LookAtTargetHandExecutor(),
        LookAtFaceDownCreatureExecutor(),
        RevealHandEffectExecutor()
    )
}
