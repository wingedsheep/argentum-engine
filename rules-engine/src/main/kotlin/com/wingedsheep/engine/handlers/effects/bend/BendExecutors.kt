package com.wingedsheep.engine.handlers.effects.bend

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing the executors for the elemental bending keyword actions of *Avatar: The Last
 * Airbender* (CR 701.65–701.67 / 702.189).
 */
class BendExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        EmitBendEventExecutor()
    )
}
