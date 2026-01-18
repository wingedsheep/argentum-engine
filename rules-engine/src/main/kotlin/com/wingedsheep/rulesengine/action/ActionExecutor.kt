package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.casting.ManaPaymentValidator
import com.wingedsheep.rulesengine.casting.SpellTimingValidator
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Executes actions and produces new game states.
 * All operations are pure functions - no side effects.
 */
object ActionExecutor {

    fun execute(state: GameState, action: Action): ActionResult {
        return try {
            val (newState, events) = executeAction(state, action)
            ActionResult.Success(newState, action, events)
        } catch (e: IllegalStateException) {
            ActionResult.Failure(state, action, e.message ?: "Unknown error")
        } catch (e: IllegalArgumentException) {
            ActionResult.Failure(state, action, e.message ?: "Invalid argument")
        }
    }

    fun executeAll(state: GameState, actions: List<Action>): ActionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for (action in actions) {
            val result = execute(currentState, action)
            when (result) {
                is ActionResult.Success -> {
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
                is ActionResult.Failure -> return result
            }
        }

        return ActionResult.Success(currentState, actions.lastOrNull() ?: return ActionResult.Success(state, GainLife(PlayerId.of("none"), 0)), allEvents)
    }

    private fun executeAction(state: GameState, action: Action): Pair<GameState, List<GameEvent>> {
        val events = mutableListOf<GameEvent>()

        val newState = when (action) {
            // Life actions
            is GainLife -> executeGainLife(state, action, events)
            is LoseLife -> executeLoseLife(state, action, events)
            is SetLife -> executeSetLife(state, action, events)
            is DealDamage -> executeDealDamage(state, action, events)

            // Mana actions
            is AddMana -> executeAddMana(state, action, events)
            is AddColorlessMana -> executeAddColorlessMana(state, action, events)
            is EmptyManaPool -> executeEmptyManaPool(state, action)

            // Card drawing
            is DrawCard -> executeDrawCard(state, action, events)
            is DrawSpecificCard -> executeDrawSpecificCard(state, action, events)

            // Library actions
            is ShuffleLibrary -> executeShuffleLibrary(state, action)
            is PutCardOnTopOfLibrary -> executePutCardOnTopOfLibrary(state, action)
            is PutCardOnBottomOfLibrary -> executePutCardOnBottomOfLibrary(state, action)
            is SearchLibrary -> state // Search is handled by player decision system

            // Card movement
            is MoveCard -> executeMoveCard(state, action, events)
            is PutCardOntoBattlefield -> executePutCardOntoBattlefield(state, action, events)
            is DestroyCard -> executeDestroyCard(state, action, events)
            is SacrificeCard -> executeSacrificeCard(state, action, events)
            is ExileCard -> executeExileCard(state, action, events)
            is ReturnToHand -> executeReturnToHand(state, action, events)
            is DiscardCard -> executeDiscardCard(state, action, events)

            // Tap/Untap
            is TapCard -> executeTapCard(state, action, events)
            is UntapCard -> executeUntapCard(state, action, events)
            is UntapAllPermanents -> executeUntapAllPermanents(state, action, events)

            // Combat damage
            is DealCombatDamageToPlayer -> executeDealCombatDamageToPlayer(state, action, events)
            is DealCombatDamageToCreature -> executeDealCombatDamageToCreature(state, action, events)
            is MarkDamageOnCreature -> executeMarkDamageOnCreature(state, action)
            is ClearDamageFromCreature -> executeClearDamageFromCreature(state, action)
            is ClearAllDamage -> executeClearAllDamage(state, action)

            // Counters
            is AddCounters -> executeAddCounters(state, action)
            is RemoveCounters -> executeRemoveCounters(state, action)
            is AddPoisonCounters -> executeAddPoisonCounters(state, action)

            // Summoning sickness
            is RemoveSummoningSickness -> executeRemoveSummoningSickness(state, action)
            is RemoveSummoningSicknessFromAll -> executeRemoveSummoningSicknessFromAll(state, action)

            // Land actions
            is PlayLand -> executePlayLand(state, action, events)
            is ResetLandsPlayed -> executeResetLandsPlayed(state, action)

            // Game flow
            is EndGame -> executeEndGame(state, action, events)
            is PlayerLoses -> executePlayerLoses(state, action, events)

            // Casting and stack
            is CastSpell -> executeCastSpell(state, action, events)
            is PayManaCost -> executePayManaCost(state, action, events)
            is ResolveTopOfStack -> executeResolveTopOfStack(state, action, events)
            is PassPriority -> executePassPriority(state, action, events)
            is CounterSpell -> executeCounterSpell(state, action, events)
        }

        return newState to events
    }

    // =============================================================================
    // Life Actions
    // =============================================================================

    private fun executeGainLife(state: GameState, action: GainLife, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val newLife = player.life + action.amount
        events.add(GameEvent.LifeChanged(action.playerId.value, player.life, newLife, action.amount))
        return state.updatePlayer(action.playerId) { it.gainLife(action.amount) }
    }

    private fun executeLoseLife(state: GameState, action: LoseLife, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val newLife = player.life - action.amount
        events.add(GameEvent.LifeChanged(action.playerId.value, player.life, newLife, -action.amount))
        return state.updatePlayer(action.playerId) { it.loseLife(action.amount) }
    }

    private fun executeSetLife(state: GameState, action: SetLife, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val delta = action.amount - player.life
        events.add(GameEvent.LifeChanged(action.playerId.value, player.life, action.amount, delta))
        return state.updatePlayer(action.playerId) { it.setLife(action.amount) }
    }

    private fun executeDealDamage(state: GameState, action: DealDamage, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.targetPlayerId)
        events.add(GameEvent.DamageDealt(action.sourceCardId?.value, action.targetPlayerId.value, action.amount, true))
        events.add(GameEvent.LifeChanged(action.targetPlayerId.value, player.life, player.life - action.amount, -action.amount))
        return state.updatePlayer(action.targetPlayerId) { it.dealDamage(action.amount) }
    }

    // =============================================================================
    // Mana Actions
    // =============================================================================

    private fun executeAddMana(state: GameState, action: AddMana, events: MutableList<GameEvent>): GameState {
        events.add(GameEvent.ManaAdded(action.playerId.value, action.color.displayName, action.amount))
        return state.updatePlayer(action.playerId) { it.addMana(action.color, action.amount) }
    }

    private fun executeAddColorlessMana(state: GameState, action: AddColorlessMana, events: MutableList<GameEvent>): GameState {
        events.add(GameEvent.ManaAdded(action.playerId.value, "Colorless", action.amount))
        return state.updatePlayer(action.playerId) { it.addColorlessMana(action.amount) }
    }

    private fun executeEmptyManaPool(state: GameState, action: EmptyManaPool): GameState {
        return state.updatePlayer(action.playerId) { it.emptyManaPool() }
    }

    // =============================================================================
    // Card Drawing
    // =============================================================================

    private fun executeDrawCard(state: GameState, action: DrawCard, events: MutableList<GameEvent>): GameState {
        var currentState = state
        repeat(action.count) {
            val player = currentState.getPlayer(action.playerId)
            val (card, newLibrary) = player.library.removeTop()

            if (card != null) {
                events.add(GameEvent.CardDrawn(action.playerId.value, card.id.value, card.name))
                currentState = currentState.updatePlayer(action.playerId) { p ->
                    p.copy(
                        library = newLibrary,
                        hand = p.hand.addToTop(card)
                    )
                }
            }
        }
        return currentState
    }

    private fun executeDrawSpecificCard(state: GameState, action: DrawSpecificCard, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val card = player.library.getCard(action.cardId)
            ?: throw IllegalStateException("Card not found in library")

        events.add(GameEvent.CardDrawn(action.playerId.value, card.id.value, card.name))
        return state.updatePlayer(action.playerId) { p ->
            p.copy(
                library = p.library.remove(action.cardId),
                hand = p.hand.addToTop(card)
            )
        }
    }

    // =============================================================================
    // Library Actions
    // =============================================================================

    private fun executeShuffleLibrary(state: GameState, action: ShuffleLibrary): GameState {
        return state.updatePlayer(action.playerId) { p ->
            p.copy(library = p.library.shuffle())
        }
    }

    private fun executePutCardOnTopOfLibrary(state: GameState, action: PutCardOnTopOfLibrary): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, location.zone, location.owner)
        return stateWithoutCard.updatePlayer(action.ownerId) { p ->
            p.copy(library = p.library.addToTop(location.card))
        }
    }

    private fun executePutCardOnBottomOfLibrary(state: GameState, action: PutCardOnBottomOfLibrary): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, location.zone, location.owner)
        return stateWithoutCard.updatePlayer(action.ownerId) { p ->
            p.copy(library = p.library.addToBottom(location.card))
        }
    }

    // =============================================================================
    // Card Movement
    // =============================================================================

    private fun executeMoveCard(state: GameState, action: MoveCard, events: MutableList<GameEvent>): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        events.add(GameEvent.CardMoved(
            location.card.id.value,
            location.card.name,
            action.fromZone.name,
            action.toZone.name
        ))

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, action.fromZone, action.fromOwnerId)
        return addCardToZone(stateWithoutCard, location.card, action.toZone, action.toOwnerId, action.toTop)
    }

    private fun executePutCardOntoBattlefield(state: GameState, action: PutCardOntoBattlefield, events: MutableList<GameEvent>): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        events.add(GameEvent.CardMoved(
            location.card.id.value,
            location.card.name,
            location.zone.name,
            ZoneType.BATTLEFIELD.name
        ))

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, location.zone, location.owner)
        val cardForBattlefield = location.card
            .copy(
                controllerId = action.controllerId.value,
                isTapped = action.tapped,
                summoningSickness = location.card.isCreature
            )

        return stateWithoutCard.updateBattlefield { it.addToTop(cardForBattlefield) }
    }

    private fun executeDestroyCard(state: GameState, action: DestroyCard, events: MutableList<GameEvent>): GameState {
        val card = state.battlefield.getCard(action.cardId)
            ?: throw IllegalStateException("Card not on battlefield")

        val ownerId = PlayerId.of(card.ownerId)
        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.BATTLEFIELD.name, ZoneType.GRAVEYARD.name))
        if (card.isCreature) {
            events.add(GameEvent.CreatureDied(card.id.value, card.name, card.ownerId))
        }

        return state
            .updateBattlefield { it.remove(action.cardId) }
            .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(card.clearDamage()) } }
    }

    private fun executeSacrificeCard(state: GameState, action: SacrificeCard, events: MutableList<GameEvent>): GameState {
        return executeDestroyCard(state, DestroyCard(action.cardId), events)
    }

    private fun executeExileCard(state: GameState, action: ExileCard, events: MutableList<GameEvent>): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        events.add(GameEvent.CardMoved(location.card.id.value, location.card.name, location.zone.name, ZoneType.EXILE.name))

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, location.zone, location.owner)
        return stateWithoutCard.updateExile { it.addToTop(location.card) }
    }

    private fun executeReturnToHand(state: GameState, action: ReturnToHand, events: MutableList<GameEvent>): GameState {
        val location = state.findCard(action.cardId)
            ?: throw IllegalStateException("Card not found")

        events.add(GameEvent.CardMoved(location.card.id.value, location.card.name, location.zone.name, ZoneType.HAND.name))

        val stateWithoutCard = removeCardFromCurrentZone(state, location.card, location.zone, location.owner)
        return stateWithoutCard.updatePlayer(action.ownerId) { p ->
            p.updateHand { it.addToTop(location.card) }
        }
    }

    private fun executeDiscardCard(state: GameState, action: DiscardCard, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val card = player.hand.getCard(action.cardId)
            ?: throw IllegalStateException("Card not in hand")

        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.HAND.name, ZoneType.GRAVEYARD.name))

        return state.updatePlayer(action.playerId) { p ->
            p.copy(
                hand = p.hand.remove(action.cardId),
                graveyard = p.graveyard.addToTop(card)
            )
        }
    }

    // =============================================================================
    // Tap/Untap
    // =============================================================================

    private fun executeTapCard(state: GameState, action: TapCard, events: MutableList<GameEvent>): GameState {
        val card = state.battlefield.getCard(action.cardId)
            ?: throw IllegalStateException("Card not on battlefield")

        events.add(GameEvent.CardTapped(card.id.value, card.name))
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.tap() }
        }
    }

    private fun executeUntapCard(state: GameState, action: UntapCard, events: MutableList<GameEvent>): GameState {
        val card = state.battlefield.getCard(action.cardId)
            ?: throw IllegalStateException("Card not on battlefield")

        events.add(GameEvent.CardUntapped(card.id.value, card.name))
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.untap() }
        }
    }

    private fun executeUntapAllPermanents(state: GameState, action: UntapAllPermanents, events: MutableList<GameEvent>): GameState {
        val permanents = state.getPermanentsControlledBy(action.playerId)
        var newState = state

        for (permanent in permanents) {
            if (permanent.isTapped) {
                events.add(GameEvent.CardUntapped(permanent.id.value, permanent.name))
                newState = newState.updateBattlefield { zone ->
                    zone.updateCard(permanent.id) { it.untap() }
                }
            }
        }

        return newState
    }

    // =============================================================================
    // Combat Damage
    // =============================================================================

    private fun executeDealCombatDamageToPlayer(state: GameState, action: DealCombatDamageToPlayer, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.defendingPlayerId)
        events.add(GameEvent.DamageDealt(action.attackerId.value, action.defendingPlayerId.value, action.amount, true))
        events.add(GameEvent.LifeChanged(action.defendingPlayerId.value, player.life, player.life - action.amount, -action.amount))
        return state.updatePlayer(action.defendingPlayerId) { it.dealDamage(action.amount) }
    }

    private fun executeDealCombatDamageToCreature(state: GameState, action: DealCombatDamageToCreature, events: MutableList<GameEvent>): GameState {
        events.add(GameEvent.DamageDealt(action.sourceId.value, action.targetId.value, action.amount, false))
        return state.updateBattlefield { zone ->
            zone.updateCard(action.targetId) { it.dealDamage(action.amount) }
        }
    }

    private fun executeMarkDamageOnCreature(state: GameState, action: MarkDamageOnCreature): GameState {
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.dealDamage(action.amount) }
        }
    }

    private fun executeClearDamageFromCreature(state: GameState, action: ClearDamageFromCreature): GameState {
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.clearDamage() }
        }
    }

    private fun executeClearAllDamage(state: GameState, action: ClearAllDamage): GameState {
        return state.updateBattlefield { zone ->
            zone.copy(cards = zone.cards.map { card ->
                if (card.isCreature && card.damageMarked > 0) {
                    card.clearDamage()
                } else {
                    card
                }
            })
        }
    }

    // =============================================================================
    // Counters
    // =============================================================================

    private fun executeAddCounters(state: GameState, action: AddCounters): GameState {
        val counterType = com.wingedsheep.rulesengine.card.CounterType.valueOf(action.counterType.uppercase().replace(" ", "_"))
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.addCounter(counterType, action.amount) }
        }
    }

    private fun executeRemoveCounters(state: GameState, action: RemoveCounters): GameState {
        val counterType = com.wingedsheep.rulesengine.card.CounterType.valueOf(action.counterType.uppercase().replace(" ", "_"))
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.removeCounter(counterType, action.amount) }
        }
    }

    private fun executeAddPoisonCounters(state: GameState, action: AddPoisonCounters): GameState {
        return state.updatePlayer(action.playerId) { it.addPoisonCounters(action.amount) }
    }

    // =============================================================================
    // Summoning Sickness
    // =============================================================================

    private fun executeRemoveSummoningSickness(state: GameState, action: RemoveSummoningSickness): GameState {
        return state.updateBattlefield { zone ->
            zone.updateCard(action.cardId) { it.removeSummoningSickness() }
        }
    }

    private fun executeRemoveSummoningSicknessFromAll(state: GameState, action: RemoveSummoningSicknessFromAll): GameState {
        val creatures = state.getCreaturesControlledBy(action.playerId)
        var newState = state

        for (creature in creatures) {
            if (creature.summoningSickness) {
                newState = newState.updateBattlefield { zone ->
                    zone.updateCard(creature.id) { it.removeSummoningSickness() }
                }
            }
        }

        return newState
    }

    // =============================================================================
    // Land Actions
    // =============================================================================

    private fun executePlayLand(state: GameState, action: PlayLand, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        if (!player.canPlayLand) {
            throw IllegalStateException("Cannot play another land this turn")
        }

        val card = player.hand.getCard(action.cardId)
            ?: throw IllegalStateException("Card not in hand")

        if (!card.isLand) {
            throw IllegalStateException("Card is not a land")
        }

        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.HAND.name, ZoneType.BATTLEFIELD.name))

        val cardForBattlefield = card.copy(controllerId = action.playerId.value, summoningSickness = false)

        return state
            .updatePlayer(action.playerId) { p ->
                p.copy(
                    hand = p.hand.remove(action.cardId),
                    landsPlayedThisTurn = p.landsPlayedThisTurn + 1
                )
            }
            .updateBattlefield { it.addToTop(cardForBattlefield) }
    }

    private fun executeResetLandsPlayed(state: GameState, action: ResetLandsPlayed): GameState {
        return state.updatePlayer(action.playerId) { it.resetLandsPlayed() }
    }

    // =============================================================================
    // Game Flow
    // =============================================================================

    private fun executeEndGame(state: GameState, action: EndGame, events: MutableList<GameEvent>): GameState {
        events.add(GameEvent.GameEnded(action.winnerId?.value))
        return state.endGame(action.winnerId)
    }

    private fun executePlayerLoses(state: GameState, action: PlayerLoses, events: MutableList<GameEvent>): GameState {
        events.add(GameEvent.PlayerLost(action.playerId.value, action.reason))
        return state.updatePlayer(action.playerId) { it.markAsLost() }
    }

    // =============================================================================
    // Helper Functions
    // =============================================================================

    private fun removeCardFromCurrentZone(
        state: GameState,
        card: CardInstance,
        zone: ZoneType,
        ownerId: PlayerId?
    ): GameState {
        return when (zone) {
            ZoneType.BATTLEFIELD -> state.updateBattlefield { it.remove(card.id) }
            ZoneType.STACK -> state.updateStack { it.remove(card.id) }
            ZoneType.EXILE -> state.updateExile { it.remove(card.id) }
            ZoneType.LIBRARY -> {
                val owner = ownerId ?: throw IllegalStateException("Library zone requires owner")
                state.updatePlayer(owner) { p -> p.updateLibrary { it.remove(card.id) } }
            }
            ZoneType.HAND -> {
                val owner = ownerId ?: throw IllegalStateException("Hand zone requires owner")
                state.updatePlayer(owner) { p -> p.updateHand { it.remove(card.id) } }
            }
            ZoneType.GRAVEYARD -> {
                val owner = ownerId ?: throw IllegalStateException("Graveyard zone requires owner")
                state.updatePlayer(owner) { p -> p.updateGraveyard { it.remove(card.id) } }
            }
            ZoneType.COMMAND -> state // Command zone not fully implemented
        }
    }

    private fun addCardToZone(
        state: GameState,
        card: CardInstance,
        zone: ZoneType,
        ownerId: PlayerId?,
        toTop: Boolean = true
    ): GameState {
        val addFunc: (com.wingedsheep.rulesengine.zone.Zone) -> com.wingedsheep.rulesengine.zone.Zone = { z ->
            if (toTop) z.addToTop(card) else z.addToBottom(card)
        }

        return when (zone) {
            ZoneType.BATTLEFIELD -> state.updateBattlefield(addFunc)
            ZoneType.STACK -> state.updateStack(addFunc)
            ZoneType.EXILE -> state.updateExile(addFunc)
            ZoneType.LIBRARY -> {
                val owner = ownerId ?: throw IllegalStateException("Library zone requires owner")
                state.updatePlayer(owner) { p -> p.updateLibrary(addFunc) }
            }
            ZoneType.HAND -> {
                val owner = ownerId ?: throw IllegalStateException("Hand zone requires owner")
                state.updatePlayer(owner) { p -> p.updateHand(addFunc) }
            }
            ZoneType.GRAVEYARD -> {
                val owner = ownerId ?: throw IllegalStateException("Graveyard zone requires owner")
                state.updatePlayer(owner) { p -> p.updateGraveyard(addFunc) }
            }
            ZoneType.COMMAND -> state // Command zone not fully implemented
        }
    }

    // =============================================================================
    // Casting and Stack Actions
    // =============================================================================

    private fun executeCastSpell(state: GameState, action: CastSpell, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        val card = player.hand.getCard(action.cardId)
            ?: throw IllegalStateException("Card not in hand")

        // Validate timing
        val timingResult = SpellTimingValidator.canCast(state, card, action.playerId)
        if (timingResult is SpellTimingValidator.TimingResult.Invalid) {
            throw IllegalStateException(timingResult.reason)
        }

        // Validate mana payment
        val paymentResult = ManaPaymentValidator.canPay(state, card, action.playerId)
        if (paymentResult is ManaPaymentValidator.PaymentResult.Invalid) {
            throw IllegalStateException(paymentResult.reason)
        }

        // Pay the mana cost
        val cost = card.definition.manaCost
        var updatedPool = player.manaPool

        // Spend colored mana
        for (color in Color.entries) {
            val needed = cost.colorCount[color] ?: 0
            if (needed > 0) {
                updatedPool = updatedPool.spend(color, needed)
            }
        }

        // Spend colorless mana
        val colorlessNeeded = cost.colorlessAmount
        if (colorlessNeeded > 0) {
            updatedPool = updatedPool.spendColorless(colorlessNeeded)
        }

        // Spend generic mana
        val genericNeeded = cost.genericAmount
        if (genericNeeded > 0) {
            updatedPool = updatedPool.spendGeneric(genericNeeded)
        }

        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.HAND.name, ZoneType.STACK.name))

        // Move card from hand to stack and update mana pool
        return state
            .updatePlayer(action.playerId) { p ->
                p.copy(
                    hand = p.hand.remove(action.cardId),
                    manaPool = updatedPool
                )
            }
            .updateStack { it.addToTop(card) }
    }

    private fun executePayManaCost(state: GameState, action: PayManaCost, events: MutableList<GameEvent>): GameState {
        val player = state.getPlayer(action.playerId)
        var updatedPool = player.manaPool

        // Spend colored mana
        if (action.white > 0) updatedPool = updatedPool.spend(Color.WHITE, action.white)
        if (action.blue > 0) updatedPool = updatedPool.spend(Color.BLUE, action.blue)
        if (action.black > 0) updatedPool = updatedPool.spend(Color.BLACK, action.black)
        if (action.red > 0) updatedPool = updatedPool.spend(Color.RED, action.red)
        if (action.green > 0) updatedPool = updatedPool.spend(Color.GREEN, action.green)
        if (action.colorless > 0) updatedPool = updatedPool.spendColorless(action.colorless)
        if (action.generic > 0) updatedPool = updatedPool.spendGeneric(action.generic)

        return state.updatePlayer(action.playerId) { it.copy(manaPool = updatedPool) }
    }

    private fun executeResolveTopOfStack(state: GameState, action: ResolveTopOfStack, events: MutableList<GameEvent>): GameState {
        if (state.stack.isEmpty) {
            throw IllegalStateException("Stack is empty")
        }

        val (cardOrNull, newStack) = state.stack.removeTop()
        val card = cardOrNull ?: throw IllegalStateException("Failed to remove card from stack")

        val ownerId = PlayerId.of(card.ownerId)

        // Determine what happens when the spell resolves
        return if (card.definition.isPermanent) {
            // Permanent spells go to the battlefield
            events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.STACK.name, ZoneType.BATTLEFIELD.name))

            val cardOnBattlefield = card.copy(
                controllerId = card.ownerId,
                summoningSickness = card.isCreature
            )

            state
                .updateStack { newStack }
                .updateBattlefield { it.addToTop(cardOnBattlefield) }
                .updateTurnState { it.resetPriorityToActivePlayer() }
        } else {
            // Non-permanent spells (instants, sorceries) go to graveyard
            events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.STACK.name, ZoneType.GRAVEYARD.name))

            state
                .updateStack { newStack }
                .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(card) } }
                .updateTurnState { it.resetPriorityToActivePlayer() }
        }
    }

    private fun executePassPriority(state: GameState, action: PassPriority, events: MutableList<GameEvent>): GameState {
        // Verify the player actually has priority
        if (state.turnState.priorityPlayer != action.playerId) {
            throw IllegalStateException("${action.playerId.value} does not have priority")
        }

        return state.passPriority()
    }

    private fun executeCounterSpell(state: GameState, action: CounterSpell, events: MutableList<GameEvent>): GameState {
        val card = state.stack.getCard(action.cardId)
            ?: throw IllegalStateException("Card not on stack")

        val ownerId = PlayerId.of(card.ownerId)

        events.add(GameEvent.CardMoved(card.id.value, card.name, ZoneType.STACK.name, ZoneType.GRAVEYARD.name))

        return state
            .updateStack { it.remove(action.cardId) }
            .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(card) } }
    }
}
