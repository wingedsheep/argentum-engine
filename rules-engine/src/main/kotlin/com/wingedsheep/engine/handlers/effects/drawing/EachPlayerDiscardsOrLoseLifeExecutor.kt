package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerDiscardsOrLoseLifeEffect.
 *
 * Handles "Each player discards a card. Then each player who didn't discard
 * a creature card this way loses N life." (Strongarm Tactics)
 *
 * Players discard in APNAP order. After all players have discarded,
 * life loss is applied to those who didn't discard a creature card.
 * Players with empty hands are treated as not discarding a creature.
 */
class EachPlayerDiscardsOrLoseLifeExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<EachPlayerDiscardsOrLoseLifeEffect> {

    override val effectType: KClass<EachPlayerDiscardsOrLoseLifeEffect> = EachPlayerDiscardsOrLoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerDiscardsOrLoseLifeEffect,
        context: EffectContext
    ): EffectResult {
        val players = state.apnapOrder
        val targets = context.targets + players.map { ChosenTarget.Player(it) }
        val effects = mutableListOf<Effect>()
        val afterDiscards = mutableListOf<Effect>()
        for ((i, _) in players.withIndex()) {
            val player = Player.ContextPlayer(context.targets.size + i)
            val slot = "discard_or_life_$i"
            val creatures = "discard_or_life_creatures_$i"
            effects += GatherCardsEffect(
                CardSource.FromZone(Zone.HAND, player), "${slot}_hand")
            effects += SelectFromCollectionEffect(
                from = "${slot}_hand", selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.ControllerOfSelection,
                storeSelected = slot, prompt = "Choose a card to discard")
            effects += MoveCollectionEffect(
                from = slot, destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                moveType = MoveType.Discard)
            effects += FilterCollectionEffect(
                from = slot, filter = GameObjectFilter.Creature, storeMatching = creatures)
            afterDiscards += ConditionalOnCollectionEffect(
                collection = creatures,
                ifNotEmpty = CompositeEffect(emptyList()),
                ifEmpty = Effects.LoseLife(effect.lifeLoss,
                    EffectTarget.ContextTarget(context.targets.size + i)))
        }
        return effectExecutor(state, CompositeEffect(effects + afterDiscards), context.copy(targets = targets))
    }

    companion object {
        /**
         * Apply life loss to all players who didn't discard a creature card.
         */
        fun applyLifeLoss(
            state: GameState,
            discardedCreature: Map<EntityId, Boolean>,
            lifeLoss: Int
        ): EffectResult {
            var currentState = state
            val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

            for ((playerId, discardedCreatureCard) in discardedCreature) {
                if (!discardedCreatureCard) {
                    if (currentState.getEntity(playerId)
                            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>() == null
                    ) continue
                    // A player whose life total is locked cannot lose life.
                    if (currentState.isLifeLossLocked(playerId)) continue
                    // Shared-team games apply life loss to the team total.
                    val currentLife = currentState.lifeTotal(playerId)
                    val newLife = currentLife - lifeLoss
                    currentState = currentState.withLifeTotal(playerId, newLife)
                    events.add(
                        com.wingedsheep.engine.core.LifeChangedEvent(
                            playerId, currentLife, newLife,
                            com.wingedsheep.engine.core.LifeChangeReason.LIFE_LOSS
                        )
                    )
                    currentState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(currentState, playerId, currentLife - newLife)
                }
            }

            return EffectResult.success(currentState, events)
        }
    }
}
