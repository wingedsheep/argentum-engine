package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.action.GameEvent
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Executes effects from triggered and activated abilities.
 */
object EffectExecutor {

    /**
     * Execute an effect and return the new game state.
     * @param state Current game state
     * @param effect The effect to execute
     * @param controllerId The player who controls the ability
     * @param sourceId The card that is the source of the ability
     * @param targets Chosen targets for the effect (if any)
     * @param events Mutable list to add generated events
     */
    fun execute(
        state: GameState,
        effect: Effect,
        controllerId: PlayerId,
        sourceId: CardId,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        return when (effect) {
            is GainLifeEffect -> executeGainLife(state, effect, controllerId, events)
            is LoseLifeEffect -> executeLoseLife(state, effect, controllerId, events)
            is DealDamageEffect -> executeDealDamage(state, effect, controllerId, sourceId, targets, events)
            is DrawCardsEffect -> executeDrawCards(state, effect, controllerId, events)
            is DiscardCardsEffect -> executeDiscardCards(state, effect, controllerId, events)
            is DestroyEffect -> executeDestroy(state, effect, targets, events)
            is ExileEffect -> executeExile(state, effect, targets, events)
            is ReturnToHandEffect -> executeReturnToHand(state, effect, targets, events)
            is TapUntapEffect -> executeTapUntap(state, effect, targets, events)
            is ModifyStatsEffect -> executeModifyStats(state, effect, targets)
            is AddCountersEffect -> executeAddCounters(state, effect, sourceId, targets)
            is AddManaEffect -> executeAddMana(state, effect, controllerId, events)
            is AddColorlessManaEffect -> executeAddColorlessMana(state, effect, controllerId, events)
            is CreateTokenEffect -> executeCreateToken(state, effect, controllerId, events)
            is CompositeEffect -> executeComposite(state, effect, controllerId, sourceId, targets, events)
        }
    }

    private fun executeGainLife(
        state: GameState,
        effect: GainLifeEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        val targetPlayerId = resolvePlayerTarget(effect.target, controllerId, state)
        val player = state.getPlayer(targetPlayerId)
        val newLife = player.life + effect.amount
        events.add(GameEvent.LifeChanged(targetPlayerId.value, player.life, newLife, effect.amount))
        return state.updatePlayer(targetPlayerId) { it.gainLife(effect.amount) }
    }

    private fun executeLoseLife(
        state: GameState,
        effect: LoseLifeEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        val targetPlayerId = resolvePlayerTarget(effect.target, controllerId, state)
        val player = state.getPlayer(targetPlayerId)
        val newLife = player.life - effect.amount
        events.add(GameEvent.LifeChanged(targetPlayerId.value, player.life, newLife, -effect.amount))
        return state.updatePlayer(targetPlayerId) { it.loseLife(effect.amount) }
    }

    private fun executeDealDamage(
        state: GameState,
        effect: DealDamageEffect,
        controllerId: PlayerId,
        sourceId: CardId,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        // Find the target from chosen targets or resolve automatically
        return when (val target = effect.target) {
            is EffectTarget.Controller -> {
                val player = state.getPlayer(controllerId)
                events.add(GameEvent.DamageDealt(sourceId.value, controllerId.value, effect.amount, true))
                events.add(GameEvent.LifeChanged(controllerId.value, player.life, player.life - effect.amount, -effect.amount))
                state.updatePlayer(controllerId) { it.dealDamage(effect.amount) }
            }
            is EffectTarget.Opponent -> {
                val opponentId = getOpponent(controllerId, state)
                val opponent = state.getPlayer(opponentId)
                events.add(GameEvent.DamageDealt(sourceId.value, opponentId.value, effect.amount, true))
                events.add(GameEvent.LifeChanged(opponentId.value, opponent.life, opponent.life - effect.amount, -effect.amount))
                state.updatePlayer(opponentId) { it.dealDamage(effect.amount) }
            }
            is EffectTarget.EachOpponent -> {
                var currentState = state
                for ((playerId, player) in state.players) {
                    if (playerId != controllerId) {
                        events.add(GameEvent.DamageDealt(sourceId.value, playerId.value, effect.amount, true))
                        events.add(GameEvent.LifeChanged(playerId.value, player.life, player.life - effect.amount, -effect.amount))
                        currentState = currentState.updatePlayer(playerId) { it.dealDamage(effect.amount) }
                    }
                }
                currentState
            }
            is EffectTarget.AnyTarget, is EffectTarget.TargetCreature, is EffectTarget.AnyPlayer -> {
                // Use chosen target
                val chosenTarget = targets.firstOrNull()
                when (chosenTarget) {
                    is ChosenTarget.PlayerTarget -> {
                        val player = state.getPlayer(chosenTarget.playerId)
                        events.add(GameEvent.DamageDealt(sourceId.value, chosenTarget.playerId.value, effect.amount, true))
                        events.add(GameEvent.LifeChanged(chosenTarget.playerId.value, player.life, player.life - effect.amount, -effect.amount))
                        state.updatePlayer(chosenTarget.playerId) { it.dealDamage(effect.amount) }
                    }
                    is ChosenTarget.CardTarget -> {
                        events.add(GameEvent.DamageDealt(sourceId.value, chosenTarget.cardId.value, effect.amount, false))
                        state.updateBattlefield { zone ->
                            zone.updateCard(chosenTarget.cardId) { it.dealDamage(effect.amount) }
                        }
                    }
                    null -> state // No target chosen, effect fizzles
                }
            }
            else -> state
        }
    }

    private fun executeDrawCards(
        state: GameState,
        effect: DrawCardsEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        val targetPlayerId = resolvePlayerTarget(effect.target, controllerId, state)
        var currentState = state

        repeat(effect.count) {
            val player = currentState.getPlayer(targetPlayerId)
            val (card, newLibrary) = player.library.removeTop()

            if (card != null) {
                events.add(GameEvent.CardDrawn(targetPlayerId.value, card.id.value, card.name))
                currentState = currentState.updatePlayer(targetPlayerId) { p ->
                    p.copy(
                        library = newLibrary,
                        hand = p.hand.addToTop(card)
                    )
                }
            }
        }

        return currentState
    }

    private fun executeDiscardCards(
        state: GameState,
        effect: DiscardCardsEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        val targetPlayerId = resolvePlayerTarget(effect.target, controllerId, state)
        var currentState = state

        // For now, discard from end of hand (would need player choice in real impl)
        repeat(effect.count) {
            val player = currentState.getPlayer(targetPlayerId)
            if (player.hand.cards.isNotEmpty()) {
                val card = player.hand.cards.last()
                events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.HAND.name, ZoneType.GRAVEYARD.name))
                currentState = currentState.updatePlayer(targetPlayerId) { p ->
                    p.copy(
                        hand = p.hand.remove(card.id),
                        graveyard = p.graveyard.addToTop(card)
                    )
                }
            }
        }

        return currentState
    }

    private fun executeDestroy(
        state: GameState,
        effect: DestroyEffect,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardTarget = targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()
            ?: return state

        val card = state.battlefield.getCard(cardTarget.cardId) ?: return state
        val ownerId = PlayerId.of(card.ownerId)

        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.BATTLEFIELD.name, ZoneType.GRAVEYARD.name))
        if (card.isCreature) {
            events.add(GameEvent.CreatureDied(card.id.value, card.name, card.ownerId))
        }

        return state
            .updateBattlefield { it.remove(cardTarget.cardId) }
            .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(card.clearDamage()) } }
    }

    private fun executeExile(
        state: GameState,
        effect: ExileEffect,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardTarget = targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()
            ?: return state

        val location = state.findCard(cardTarget.cardId) ?: return state

        events.add(GameEvent.CardMoved(location.card.id.value, location.card.name, location.zone.name, ZoneType.EXILE.name))

        return when (location.zone) {
            ZoneType.BATTLEFIELD -> state
                .updateBattlefield { it.remove(cardTarget.cardId) }
                .updateExile { it.addToTop(location.card) }
            ZoneType.GRAVEYARD -> {
                val ownerId = location.owner ?: return state
                state
                    .updatePlayer(ownerId) { p -> p.updateGraveyard { it.remove(cardTarget.cardId) } }
                    .updateExile { it.addToTop(location.card) }
            }
            else -> state
        }
    }

    private fun executeReturnToHand(
        state: GameState,
        effect: ReturnToHandEffect,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardTarget = targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()
            ?: return state

        val location = state.findCard(cardTarget.cardId) ?: return state
        val ownerId = PlayerId.of(location.card.ownerId)

        events.add(GameEvent.CardMoved(location.card.id.value, location.card.name, location.zone.name, ZoneType.HAND.name))

        return when (location.zone) {
            ZoneType.BATTLEFIELD -> state
                .updateBattlefield { it.remove(cardTarget.cardId) }
                .updatePlayer(ownerId) { p -> p.updateHand { it.addToTop(location.card) } }
            ZoneType.GRAVEYARD -> {
                val graveyardOwner = location.owner ?: return state
                state
                    .updatePlayer(graveyardOwner) { p -> p.updateGraveyard { it.remove(cardTarget.cardId) } }
                    .updatePlayer(ownerId) { p -> p.updateHand { it.addToTop(location.card) } }
            }
            else -> state
        }
    }

    private fun executeTapUntap(
        state: GameState,
        effect: TapUntapEffect,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardTarget = targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()
            ?: return state

        val card = state.battlefield.getCard(cardTarget.cardId) ?: return state

        return if (effect.tap) {
            events.add(GameEvent.CardTapped(card.id.value, card.name))
            state.updateBattlefield { zone -> zone.updateCard(cardTarget.cardId) { it.tap() } }
        } else {
            events.add(GameEvent.CardUntapped(card.id.value, card.name))
            state.updateBattlefield { zone -> zone.updateCard(cardTarget.cardId) { it.untap() } }
        }
    }

    private fun executeModifyStats(
        state: GameState,
        effect: ModifyStatsEffect,
        targets: List<ChosenTarget>
    ): GameState {
        val cardTarget = targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()
            ?: return state

        return state.updateBattlefield { zone ->
            zone.updateCard(cardTarget.cardId) { card ->
                card.modifyPower(effect.powerModifier).modifyToughness(effect.toughnessModifier)
            }
        }
    }

    private fun executeAddCounters(
        state: GameState,
        effect: AddCountersEffect,
        sourceId: CardId,
        targets: List<ChosenTarget>
    ): GameState {
        val targetId = when (effect.target) {
            is EffectTarget.Self -> sourceId
            else -> (targets.filterIsInstance<ChosenTarget.CardTarget>().firstOrNull()?.cardId ?: return state)
        }

        val counterType = try {
            CounterType.valueOf(effect.counterType.uppercase().replace(" ", "_").replace("+1/+1", "PLUS_ONE_PLUS_ONE").replace("-1/-1", "MINUS_ONE_MINUS_ONE"))
        } catch (e: IllegalArgumentException) {
            return state
        }

        return state.updateBattlefield { zone ->
            zone.updateCard(targetId) { it.addCounter(counterType, effect.count) }
        }
    }

    private fun executeAddMana(
        state: GameState,
        effect: AddManaEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        events.add(GameEvent.ManaAdded(controllerId.value, effect.color.displayName, effect.amount))
        return state.updatePlayer(controllerId) { it.addMana(effect.color, effect.amount) }
    }

    private fun executeAddColorlessMana(
        state: GameState,
        effect: AddColorlessManaEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        events.add(GameEvent.ManaAdded(controllerId.value, "Colorless", effect.amount))
        return state.updatePlayer(controllerId) { it.addColorlessMana(effect.amount) }
    }

    private fun executeCreateToken(
        state: GameState,
        effect: CreateTokenEffect,
        controllerId: PlayerId,
        events: MutableList<GameEvent>
    ): GameState {
        // Token creation would need CardDefinition for the token
        // This is a placeholder - full implementation in Phase 12
        return state
    }

    private fun executeComposite(
        state: GameState,
        effect: CompositeEffect,
        controllerId: PlayerId,
        sourceId: CardId,
        targets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        var currentState = state
        for (subEffect in effect.effects) {
            currentState = execute(currentState, subEffect, controllerId, sourceId, targets, events)
        }
        return currentState
    }

    // =============================================================================
    // Helper Functions
    // =============================================================================

    private fun resolvePlayerTarget(target: EffectTarget, controllerId: PlayerId, state: GameState): PlayerId {
        return when (target) {
            is EffectTarget.Controller -> controllerId
            is EffectTarget.Opponent -> getOpponent(controllerId, state)
            else -> controllerId
        }
    }

    private fun getOpponent(playerId: PlayerId, state: GameState): PlayerId {
        return state.players.keys.first { it != playerId }
    }
}
