package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.actions.ActionHandlerModule

/**
 * Module providing handlers for ability actions.
 *
 * Ability actions include:
 * - ActivateAbility: Activate an ability on a permanent
 * - CycleCard: Cycle a card from hand
 * - PlotCard: Plot a card from hand (CR 718)
 */
class AbilityModule(private val services: EngineServices) : ActionHandlerModule {
    override fun handlers(): List<ActionHandler<*>> = listOf(
        ActivateAbilityHandler.create(services),
        CycleCardHandler.create(services),
        PlotCardHandler.create(services),
        ForetellCardHandler.create(services),
        TypecycleCardHandler.create(services),
        CrewVehicleHandler.create(services),
        SaddleMountHandler.create(services)
    )
}
