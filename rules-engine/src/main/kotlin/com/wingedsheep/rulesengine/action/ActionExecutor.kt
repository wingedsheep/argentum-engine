package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.casting.ManaPaymentValidator
import com.wingedsheep.rulesengine.casting.SpellTimingValidator
import com.wingedsheep.rulesengine.combat.CombatDamageResult
import com.wingedsheep.rulesengine.combat.CombatValidator
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Step
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

            // Combat
            is BeginCombat -> executeBeginCombat(state, action, events)
            is DeclareAttacker -> executeDeclareAttacker(state, action, events)
            is DeclareBlocker -> executeDeclareBlocker(state, action, events)
            is SetDamageAssignmentOrder -> executeSetDamageAssignmentOrder(state, action)
            is ResolveCombatDamage -> executeResolveCombatDamage(state, action, events)
            is EndCombat -> executeEndCombat(state, action, events)
            is CheckStateBasedActions -> executeCheckStateBasedActions(state, action, events)

            // Triggered abilities
            is PutTriggerOnStack -> executePutTriggerOnStack(state, action, events)
            is ResolveTriggeredAbility -> executeResolveTriggeredAbility(state, action, events)

            // Attachment actions
            is AttachCard -> executeAttachCard(state, action, events)
            is DetachCard -> executeDetachCard(state, action, events)
            is Equip -> executeEquip(state, action, events)
            is Unattach -> executeUnattach(state, action, events)
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
            } else {
                // Drawing from an empty library causes the player to lose
                // This is tracked via an event - the actual loss is handled by state-based actions
                events.add(GameEvent.TriedToDrawFromEmptyLibrary(action.playerId.value))
                currentState = currentState.updatePlayer(action.playerId) { p ->
                    p.copy(hasLost = true)
                }
                events.add(GameEvent.PlayerLost(action.playerId.value, "Attempted to draw from an empty library"))
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

    /**
     * Helper function to handle lifelink.
     * When a creature with lifelink deals damage, its controller gains that much life.
     */
    private fun handleLifelink(
        state: GameState,
        source: CardInstance,
        damageDealt: Int,
        events: MutableList<GameEvent>
    ): GameState {
        if (damageDealt <= 0 || !source.hasKeyword(Keyword.LIFELINK)) {
            return state
        }

        val controllerId = PlayerId.of(source.controllerId)
        val currentLife = state.getPlayer(controllerId).life
        events.add(GameEvent.LifeChanged(controllerId.value, currentLife, currentLife + damageDealt, damageDealt))
        return state.updatePlayer(controllerId) { it.gainLife(damageDealt) }
    }

    /**
     * Helper function to process blocker damage to attacker.
     * Handles first strike/double strike timing and lifelink.
     */
    private fun processBlockerDamage(
        state: GameState,
        combat: com.wingedsheep.rulesengine.combat.CombatState,
        attackerId: com.wingedsheep.rulesengine.core.CardId,
        isFirstStrikeDamageStep: Boolean,
        isRegularDamageStep: Boolean,
        events: MutableList<GameEvent>
    ): GameState {
        var currentState = state
        val blockerIds = combat.getBlockersFor(attackerId)

        for (blockerId in blockerIds) {
            val blocker = currentState.battlefield.getCard(blockerId) ?: continue

            // Check if blocker deals damage in this step
            val blockerHasFirstStrike = blocker.hasKeyword(Keyword.FIRST_STRIKE)
            val blockerHasDoubleStrike = blocker.hasKeyword(Keyword.DOUBLE_STRIKE)

            val blockerDealsDamageThisStep = when {
                isFirstStrikeDamageStep -> blockerHasFirstStrike || blockerHasDoubleStrike
                isRegularDamageStep -> !blockerHasFirstStrike || blockerHasDoubleStrike
                else -> true
            }

            if (!blockerDealsDamageThisStep) continue

            val blockerPower = blocker.currentPower ?: 0
            if (blockerPower > 0) {
                events.add(GameEvent.DamageDealt(blockerId.value, attackerId.value, blockerPower, false))
                currentState = currentState.updateBattlefield { zone ->
                    zone.updateCard(attackerId) { it.dealDamage(blockerPower) }
                }

                // Handle lifelink for blocker
                currentState = handleLifelink(currentState, blocker, blockerPower, events)
            }
        }

        return currentState
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

    // =============================================================================
    // Combat Actions
    // =============================================================================

    private fun executeBeginCombat(state: GameState, action: BeginCombat, events: MutableList<GameEvent>): GameState {
        if (state.combat != null) {
            throw IllegalStateException("Already in combat")
        }

        return state.startCombat(action.defendingPlayer)
    }

    private fun executeDeclareAttacker(state: GameState, action: DeclareAttacker, events: MutableList<GameEvent>): GameState {
        val validationResult = CombatValidator.canDeclareAttacker(state, action.cardId, action.playerId)
        if (validationResult is CombatValidator.ValidationResult.Invalid) {
            throw IllegalStateException(validationResult.reason)
        }

        val creature = state.battlefield.getCard(action.cardId)!!

        // Tap the attacker (unless has vigilance)
        val shouldTap = !creature.hasKeyword(Keyword.VIGILANCE)

        var newState = state.updateCombat { it.addAttacker(action.cardId) }

        if (shouldTap) {
            events.add(GameEvent.CardTapped(creature.id.value, creature.name))
            newState = newState.updateBattlefield { zone ->
                zone.updateCard(action.cardId) { it.tap() }
            }
        }

        return newState
    }

    private fun executeDeclareBlocker(state: GameState, action: DeclareBlocker, events: MutableList<GameEvent>): GameState {
        val validationResult = CombatValidator.canDeclareBlocker(state, action.blockerId, action.attackerId, action.playerId)
        if (validationResult is CombatValidator.ValidationResult.Invalid) {
            throw IllegalStateException(validationResult.reason)
        }

        return state.updateCombat { it.addBlocker(action.blockerId, action.attackerId) }
    }

    private fun executeSetDamageAssignmentOrder(state: GameState, action: SetDamageAssignmentOrder): GameState {
        return state.updateCombat { it.setDamageAssignmentOrder(action.attackerId, action.blockerOrder) }
    }

    private fun executeResolveCombatDamage(state: GameState, action: ResolveCombatDamage, events: MutableList<GameEvent>): GameState {
        val combat = state.combat
            ?: throw IllegalStateException("Not in combat")

        var currentState = state

        // Determine which damage step we're in
        val isFirstStrikeDamageStep = state.currentStep == Step.FIRST_STRIKE_COMBAT_DAMAGE
        val isRegularDamageStep = state.currentStep == Step.COMBAT_DAMAGE

        // If specific attacker is specified, only resolve that one
        val attackerIds = if (action.attackerId != null) {
            listOf(action.attackerId)
        } else {
            combat.attackerIds.toList()
        }

        for (attackerId in attackerIds) {
            val attacker = currentState.battlefield.getCard(attackerId) ?: continue

            // Check if this attacker deals damage in this step
            val hasFirstStrike = attacker.hasKeyword(Keyword.FIRST_STRIKE)
            val hasDoubleStrike = attacker.hasKeyword(Keyword.DOUBLE_STRIKE)

            val attackerDealsDamageThisStep = when {
                isFirstStrikeDamageStep -> hasFirstStrike || hasDoubleStrike
                isRegularDamageStep -> !hasFirstStrike || hasDoubleStrike
                else -> true // If step not specified, deal damage (for backwards compatibility)
            }

            val damageResult = CombatValidator.calculateCombatDamage(currentState, attackerId)

            when (damageResult) {
                is CombatDamageResult.UnblockedDamage -> {
                    if (attackerDealsDamageThisStep) {
                        // Deal damage to defending player
                        val damage = damageResult.damage
                        events.add(GameEvent.DamageDealt(attackerId.value, combat.defendingPlayer.value, damage, true))
                        events.add(GameEvent.LifeChanged(
                            combat.defendingPlayer.value,
                            currentState.getPlayer(combat.defendingPlayer).life,
                            currentState.getPlayer(combat.defendingPlayer).life - damage,
                            -damage
                        ))
                        currentState = currentState.updatePlayer(combat.defendingPlayer) { it.dealDamage(damage) }

                        // Handle lifelink
                        currentState = handleLifelink(currentState, attacker, damage, events)
                    }
                }
                is CombatDamageResult.BlockedDamage -> {
                    var totalDamageDealtByAttacker = 0

                    // Deal damage to blockers (only if attacker deals damage this step)
                    if (attackerDealsDamageThisStep) {
                        for ((blockerId, damage) in damageResult.damageToBlockers) {
                            if (damage > 0) {
                                val blocker = currentState.battlefield.getCard(blockerId) ?: continue
                                events.add(GameEvent.DamageDealt(attackerId.value, blockerId.value, damage, false))
                                currentState = currentState.updateBattlefield { zone ->
                                    zone.updateCard(blockerId) { it.dealDamage(damage) }
                                }
                                totalDamageDealtByAttacker += damage
                            }
                        }

                        // Trample damage goes to player
                        if (damageResult.trampleDamage > 0) {
                            events.add(GameEvent.DamageDealt(attackerId.value, combat.defendingPlayer.value, damageResult.trampleDamage, true))
                            events.add(GameEvent.LifeChanged(
                                combat.defendingPlayer.value,
                                currentState.getPlayer(combat.defendingPlayer).life,
                                currentState.getPlayer(combat.defendingPlayer).life - damageResult.trampleDamage,
                                -damageResult.trampleDamage
                            ))
                            currentState = currentState.updatePlayer(combat.defendingPlayer) { it.dealDamage(damageResult.trampleDamage) }
                            totalDamageDealtByAttacker += damageResult.trampleDamage
                        }

                        // Handle lifelink for all damage dealt by attacker
                        currentState = handleLifelink(currentState, attacker, totalDamageDealtByAttacker, events)
                    }

                    // Blockers deal damage back to attacker (independently of whether attacker dealt damage)
                    currentState = processBlockerDamage(
                        currentState, combat, attackerId, isFirstStrikeDamageStep, isRegularDamageStep, events
                    )
                }
                is CombatDamageResult.NoDamage -> {
                    // No damage from attacker, but blockers might still deal damage
                    currentState = processBlockerDamage(
                        currentState, combat, attackerId, isFirstStrikeDamageStep, isRegularDamageStep, events
                    )
                }
                is CombatDamageResult.Invalid -> {
                    // Skip invalid damage results
                }
            }
        }

        return currentState
    }

    private fun executeEndCombat(state: GameState, action: EndCombat, events: MutableList<GameEvent>): GameState {
        return state.endCombat()
    }

    private fun executeCheckStateBasedActions(state: GameState, action: CheckStateBasedActions, events: MutableList<GameEvent>): GameState {
        var currentState = state
        var actionsPerformed: Boolean

        // Keep checking until no more state-based actions are performed
        do {
            actionsPerformed = false

            // Check for creatures with lethal damage
            val creaturesWithLethalDamage = currentState.battlefield.cards
                .filter { it.isCreature && CombatValidator.hasLethalDamage(it) }

            for (creature in creaturesWithLethalDamage) {
                val ownerId = PlayerId.of(creature.ownerId)
                events.add(GameEvent.CreatureDied(creature.id.value, creature.name, creature.ownerId))
                events.add(GameEvent.CardMoved(creature.id.value, creature.name, ZoneType.BATTLEFIELD.name, ZoneType.GRAVEYARD.name))

                currentState = currentState
                    .updateBattlefield { it.remove(creature.id) }
                    .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(creature.clearDamage()) } }
                actionsPerformed = true
            }

            // Check for creatures with 0 or less toughness
            val creaturesWithZeroToughness = currentState.battlefield.cards
                .filter { it.isCreature && (it.currentToughness ?: 0) <= 0 }

            for (creature in creaturesWithZeroToughness) {
                val ownerId = PlayerId.of(creature.ownerId)
                events.add(GameEvent.CreatureDied(creature.id.value, creature.name, creature.ownerId))
                events.add(GameEvent.CardMoved(creature.id.value, creature.name, ZoneType.BATTLEFIELD.name, ZoneType.GRAVEYARD.name))

                currentState = currentState
                    .updateBattlefield { it.remove(creature.id) }
                    .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(creature) } }
                actionsPerformed = true
            }

            // Check for players with 0 or less life
            for ((playerId, player) in currentState.players) {
                if (player.life <= 0 && !player.hasLost) {
                    events.add(GameEvent.PlayerLost(playerId.value, "Life total reached 0"))
                    currentState = currentState.updatePlayer(playerId) { it.markAsLost() }
                    actionsPerformed = true
                }
            }

            // Check for players with 10+ poison counters
            for ((playerId, player) in currentState.players) {
                if (player.poisonCounters >= 10 && !player.hasLost) {
                    events.add(GameEvent.PlayerLost(playerId.value, "10 or more poison counters"))
                    currentState = currentState.updatePlayer(playerId) { it.markAsLost() }
                    actionsPerformed = true
                }
            }

            // Legendary rule: if a player controls two or more legendary permanents with the same name,
            // they must choose one to keep and put the rest into the graveyard
            // (For now, automatically keep the first one encountered)
            val legendaryPermanents = currentState.battlefield.cards
                .filter { it.definition.typeLine.supertypes.contains(com.wingedsheep.rulesengine.core.Supertype.LEGENDARY) }
                .groupBy { it.controllerId to it.name }
                .filter { it.value.size > 1 }

            for ((_, permanents) in legendaryPermanents) {
                // Keep the first one, put the rest into the graveyard
                val toSacrifice = permanents.drop(1)

                for (permanent in toSacrifice) {
                    val ownerId = PlayerId.of(permanent.ownerId)
                    events.add(GameEvent.LegendaryRuleApplied(permanent.id.value, permanent.name, permanent.controllerId))
                    events.add(GameEvent.CardMoved(permanent.id.value, permanent.name, ZoneType.BATTLEFIELD.name, ZoneType.GRAVEYARD.name))

                    currentState = currentState
                        .updateBattlefield { it.remove(permanent.id) }
                        .updatePlayer(ownerId) { p -> p.updateGraveyard { it.addToTop(permanent) } }
                    actionsPerformed = true
                }
            }

        } while (actionsPerformed)

        // Check if game should end (only one player remaining)
        val activePlayers = currentState.players.filter { !it.value.hasLost }
        if (activePlayers.size == 1) {
            val winner = activePlayers.keys.first()
            events.add(GameEvent.GameEnded(winner.value))
            currentState = currentState.endGame(winner)
        } else if (activePlayers.isEmpty()) {
            events.add(GameEvent.GameEnded(null))
            currentState = currentState.endGame(null)
        }

        return currentState
    }

    // =============================================================================
    // Triggered Ability Actions
    // =============================================================================

    private fun executePutTriggerOnStack(state: GameState, action: PutTriggerOnStack, events: MutableList<GameEvent>): GameState {
        // Find the pending trigger by ID
        val pendingTrigger = state.pendingTriggers.find { it.ability.id.value == action.triggerId }
            ?: throw IllegalStateException("Trigger not found: ${action.triggerId}")

        // Create a stacked trigger from the pending trigger
        val stackedTrigger = com.wingedsheep.rulesengine.ability.StackedTrigger(
            pendingTrigger = pendingTrigger,
            chosenTargets = emptyList() // Targets would be chosen by player decision system
        )

        // Remove from pending and add to stack
        val newPendingTriggers = state.pendingTriggers.filter { it.ability.id.value != action.triggerId }

        return state
            .copy(pendingTriggers = newPendingTriggers)
            .putTriggerOnStack(stackedTrigger)
    }

    private fun executeResolveTriggeredAbility(state: GameState, action: ResolveTriggeredAbility, events: MutableList<GameEvent>): GameState {
        // Get the top trigger from the stack
        val (stackedTrigger, stateWithoutTrigger) = state.removeTopTriggerFromStack()
            ?: throw IllegalStateException("No triggered abilities on stack")

        if (stackedTrigger == null) {
            throw IllegalStateException("No triggered abilities on stack")
        }

        // Execute the effect
        val effect = stackedTrigger.pendingTrigger.ability.effect

        return com.wingedsheep.rulesengine.ability.EffectExecutor.execute(
            state = stateWithoutTrigger,
            effect = effect,
            controllerId = stackedTrigger.controllerId,
            sourceId = stackedTrigger.sourceId,
            targets = stackedTrigger.chosenTargets,
            events = events
        ).updateTurnState { it.resetPriorityToActivePlayer() }
    }

    // =============================================================================
    // Attachment Actions
    // =============================================================================

    private fun executeAttachCard(state: GameState, action: AttachCard, events: MutableList<GameEvent>): GameState {
        val attachment = state.battlefield.getCard(action.attachmentId)
            ?: throw IllegalStateException("Attachment not found on battlefield")

        val target = state.battlefield.getCard(action.targetId)
            ?: throw IllegalStateException("Target permanent not found on battlefield")

        // Validate attachment rules
        if (attachment.isAura) {
            // Auras can attach to various permanents depending on their "Enchant X" ability
            // For now, we allow attaching to any permanent
        } else if (attachment.isEquipment) {
            // Equipment can only attach to creatures you control
            if (!target.isCreature) {
                throw IllegalStateException("Equipment can only be attached to creatures")
            }
            if (target.controllerId != attachment.controllerId) {
                throw IllegalStateException("Equipment can only be attached to creatures you control")
            }
        }

        // If already attached to something, detach first
        val stateAfterDetach = if (attachment.isAttached) {
            state.updateBattlefield { zone ->
                zone.updateCard(action.attachmentId) { it.detach() }
            }
        } else {
            state
        }

        events.add(GameEvent.CardMoved(
            cardId = action.attachmentId.value,
            cardName = attachment.name,
            fromZone = ZoneType.BATTLEFIELD.name,
            toZone = ZoneType.BATTLEFIELD.name  // Stays on battlefield but now attached
        ))

        return stateAfterDetach.updateBattlefield { zone ->
            zone.updateCard(action.attachmentId) { it.attachTo(action.targetId) }
        }
    }

    private fun executeDetachCard(state: GameState, action: DetachCard, events: MutableList<GameEvent>): GameState {
        val attachment = state.battlefield.getCard(action.attachmentId)
            ?: throw IllegalStateException("Attachment not found on battlefield")

        if (!attachment.isAttached) {
            throw IllegalStateException("Card is not attached to anything")
        }

        events.add(GameEvent.CardMoved(
            cardId = action.attachmentId.value,
            cardName = attachment.name,
            fromZone = ZoneType.BATTLEFIELD.name,
            toZone = ZoneType.BATTLEFIELD.name  // Stays on battlefield but now detached
        ))

        return state.updateBattlefield { zone ->
            zone.updateCard(action.attachmentId) { it.detach() }
        }
    }

    private fun executeEquip(state: GameState, action: Equip, events: MutableList<GameEvent>): GameState {
        val equipment = state.battlefield.getCard(action.equipmentId)
            ?: throw IllegalStateException("Equipment not found on battlefield")

        if (!equipment.isEquipment) {
            throw IllegalStateException("Card is not equipment")
        }

        if (equipment.controllerId != action.controllerId.value) {
            throw IllegalStateException("You don't control this equipment")
        }

        val target = state.battlefield.getCard(action.targetCreatureId)
            ?: throw IllegalStateException("Target creature not found on battlefield")

        if (!target.isCreature) {
            throw IllegalStateException("Target is not a creature")
        }

        if (target.controllerId != action.controllerId.value) {
            throw IllegalStateException("You can only equip creatures you control")
        }

        // Check timing - Equip is sorcery speed
        if (!state.isMainPhase || !state.stackIsEmpty) {
            throw IllegalStateException("Equip can only be activated at sorcery speed")
        }

        if (state.turnState.activePlayer != action.controllerId) {
            throw IllegalStateException("You can only equip during your turn")
        }

        // Equip cost would be paid through PayManaCost action
        // This action just performs the attachment

        // If already attached to something, detach first
        val stateAfterDetach = if (equipment.isAttached) {
            state.updateBattlefield { zone ->
                zone.updateCard(action.equipmentId) { it.detach() }
            }
        } else {
            state
        }

        events.add(GameEvent.CardMoved(
            cardId = action.equipmentId.value,
            cardName = equipment.name,
            fromZone = ZoneType.BATTLEFIELD.name,
            toZone = ZoneType.BATTLEFIELD.name  // Stays on battlefield but now equipped
        ))

        return stateAfterDetach.updateBattlefield { zone ->
            zone.updateCard(action.equipmentId) { it.attachTo(action.targetCreatureId) }
        }
    }

    private fun executeUnattach(state: GameState, action: Unattach, events: MutableList<GameEvent>): GameState {
        val attachment = state.battlefield.getCard(action.attachmentId)
            ?: throw IllegalStateException("Attachment not found on battlefield")

        if (!attachment.isAttached) {
            return state  // Already detached, nothing to do
        }

        events.add(GameEvent.CardMoved(
            cardId = action.attachmentId.value,
            cardName = attachment.name,
            fromZone = ZoneType.BATTLEFIELD.name,
            toZone = ZoneType.BATTLEFIELD.name  // Stays on battlefield but now detached
        ))

        return state.updateBattlefield { zone ->
            zone.updateCard(action.attachmentId) { it.detach() }
        }
    }
}
