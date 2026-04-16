package com.wingedsheep.ai.llm.decision

import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.ai.llm.decision.handlers.*
import kotlin.reflect.KClass

/**
 * Maps each [PendingDecision] subtype to its [AiDecisionHandler].
 *
 * To add a new decision type, create a handler in the `handlers` package
 * and register it here.
 */
class AiDecisionHandlerRegistry {

    private val handlers = mutableMapOf<KClass<out PendingDecision>, AiDecisionHandler<*>>()

    init {
        register(ChooseTargetsHandler())
        register(SelectCardsHandler())
        register(YesNoHandler())
        register(ChooseModeHandler())
        register(ChooseColorHandler())
        register(ChooseNumberHandler())
        register(DistributeHandler())
        register(OrderObjectsHandler())
        register(SplitPilesHandler())
        register(ChooseOptionHandler())
        register(BudgetModalHandler())
        register(AssignDamageHandler())
        register(SearchLibraryHandler())
        register(ReorderLibraryHandler())
        register(SelectManaSourcesHandler())
    }

    private fun <D : PendingDecision> register(handler: AiDecisionHandler<D>) {
        handlers[handler.decisionType] = handler
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : PendingDecision> getHandler(decision: D): AiDecisionHandler<D>? {
        return handlers[decision::class] as? AiDecisionHandler<D>
    }
}
