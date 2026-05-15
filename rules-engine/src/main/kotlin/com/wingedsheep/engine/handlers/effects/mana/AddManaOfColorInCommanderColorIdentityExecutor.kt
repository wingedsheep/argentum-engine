package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorInCommanderColorIdentityEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddManaOfColorInCommanderColorIdentityEffect].
 * "{T}: Add one mana of any color in your commander's color identity." (Arcane Signet)
 *
 * Available colors are the union of the [com.wingedsheep.sdk.model.CardDefinition.colorIdentity]
 * of every commander registered to the controller via [CommanderRegistryComponent]. If the
 * controller has no commander (non-Commander format), the ability resolves with no effect.
 */
class AddManaOfColorInCommanderColorIdentityExecutor(
    private val cardRegistry: CardRegistry,
) : EffectExecutor<AddManaOfColorInCommanderColorIdentityEffect> {

    override val effectType: KClass<AddManaOfColorInCommanderColorIdentityEffect> =
        AddManaOfColorInCommanderColorIdentityEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaOfColorInCommanderColorIdentityEffect,
        context: EffectContext,
    ): EffectResult {
        val availableColors = commanderColorIdentity(state, context)
        if (availableColors.isEmpty()) {
            return EffectResult.success(state)
        }

        val chosenColor = context.manaColorChoice?.takeIf { it in availableColors }
            ?: availableColors.first()

        val newState = state.updateEntity(context.controllerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updated = if (effect.restriction != null) {
                pool.addRestricted(chosenColor, 1, effect.restriction!!)
            } else {
                pool.add(chosenColor, 1)
            }
            container.with(updated)
        }
        return EffectResult.success(newState)
    }

    private fun commanderColorIdentity(state: GameState, context: EffectContext): Set<Color> {
        val registry = state.getEntity(context.controllerId)
            ?.get<CommanderRegistryComponent>()
            ?: return emptySet()

        val colors = mutableSetOf<Color>()
        for (commanderId in registry.commanderIds) {
            val card = state.getEntity(commanderId)?.get<CardComponent>() ?: continue
            val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            colors.addAll(def.colorIdentity)
        }
        return colors
    }
}
