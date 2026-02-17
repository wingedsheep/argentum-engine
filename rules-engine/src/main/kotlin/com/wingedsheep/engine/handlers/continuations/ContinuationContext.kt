package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState

typealias CheckForMore = (GameState, List<GameEvent>) -> ExecutionResult

class ContinuationContext(
    val effectExecutorRegistry: EffectExecutorRegistry,
    val stackResolver: StackResolver,
    val triggerProcessor: com.wingedsheep.engine.event.TriggerProcessor?,
    val triggerDetector: com.wingedsheep.engine.event.TriggerDetector?,
    val combatManager: CombatManager?,
    val targetFinder: TargetFinder
)
