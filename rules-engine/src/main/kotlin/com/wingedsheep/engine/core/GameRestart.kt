package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A resolved "restart the game" effect (CR 727), waiting for its resolution to finish. Recorded by
 * the `RestartGameExecutor` and carried out by the settle boundary through [GameRestarter], the same
 * way "end the turn" waits for its resolution before running (see [Settler]).
 *
 * @property startingPlayerId the controller of the restarting effect, who takes the first turn of
 *   the new game (CR 727.1a)
 * @property exempt cards left in exile instead of joining their owners' decks (CR 727.5)
 * @property followUp the rest of the resolving ability's instructions, followed just before the new
 *   game's first untap step (CR 727.4); its pipeline names the [exempt] cards by their old ids
 */
@Serializable
data class GameRestartRequest(
    val startingPlayerId: EntityId,
    val exempt: List<EntityId> = emptyList(),
    val followUp: TurnStartFollowUp? = null,
)

/**
 * Parked beneath a restart's follow-up instructions when they stop to ask a question (an
 * "as this enters, choose …" of a card put onto the battlefield). Auto-resumed once they finish, it
 * starts the new game's first turn — the step the pre-game procedure would otherwise have taken.
 */
@Serializable
data object BeginFirstTurnContinuation : AutomaticContinuation

/**
 * Restarts the game (CR 727): the current game ends with no winner, loser or draw, and every player
 * still in it begins a new one following CR 103, with the restarting effect's controller as the
 * starting player (CR 727.1a).
 *
 * Every card the players own that is in the game — in any zone, phased out or not, cast or not —
 * becomes a card of its owner's new deck (CR 727.2), its printed self again. Tokens, copies of
 * cards, abilities on the stack and emblems are not cards and do not carry over, and nothing that
 * happened in the old game applies to the new one: life totals, counters, effects, designations and
 * turn history all start fresh. Cards outside the game (the sideboard) stay there, and an exempted
 * card stays in exile (CR 727.5). A commander goes back to its command zone (CR 103.2c) unless it
 * was exempted, and remains its deck's commander (CR 727.5a). A player who already left the game is
 * not part of the new one.
 *
 * Every card in the new game is given a new entity id, minted in a shuffled order exactly as at
 * game setup, so an id the old game made public can't name a card in a new library or hand.
 *
 * The new game then begins: libraries are shuffled, each player draws seven, and the mulligan and
 * opening-hand procedure runs as at any game start. When it finishes, [GameRestartRequest.followUp]
 * runs (CR 727.4) — see [com.wingedsheep.engine.handlers.MulliganHandler.beginFirstTurn].
 */
class GameRestarter(
    private val cardRegistry: CardRegistry,
    /**
     * Starts the new game's first turn, following the restart's instructions first — for a game
     * set up without mulligans, which has no pre-game procedure to end in it (see
     * [com.wingedsheep.engine.handlers.MulliganHandler.beginFirstTurn]).
     */
    private val beginFirstTurn: ((GameState, List<GameEvent>) -> ExecutionResult)? = null,
) {

    fun restart(state: GameState, request: GameRestartRequest): ExecutionResult {
        val involved = state.turnOrder.filter { state.getEntity(it)?.has<PlayerLostComponent>() != true }
        val starting = request.startingPlayerId.takeIf { it in involved } ?: involved.first()
        val exemptInExile = request.exempt.filter { state.logicalZone(it)?.zoneType == Zone.EXILE }.toSet()

        var fresh = GameState(
            preserveGraveyardOrder = state.preserveGraveyardOrder,
            format = state.format,
            attackMode = state.attackMode,
            rng = state.rng,
            nextEntityId = state.nextEntityId,
            nextRoutingId = state.nextRoutingId,
            nextObjectGeneration = state.nextObjectGeneration,
            timestamp = state.timestamp,
            yieldsByPlayer = state.yieldsByPlayer,
        ).clearUntilEndOfTurnYields()

        val turnOrder = startingFrom(state, starting)
        fresh = fresh.copy(
            turnOrder = turnOrder,
            activePlayerId = starting,
            priorityPlayerId = starting,
            turnNumber = 1,
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
        )

        // The cards each involved player owns, in a stable order, with the zone each starts the new
        // game in. Vanguard avatars aren't cards of a deck; they keep their place in the command zone.
        val cards = linkedMapOf<EntityId, MutableList<Pair<ComponentContainer, Zone>>>()
        val oldIdsByOwner = linkedMapOf<EntityId, MutableList<EntityId>>()
        val avatars = mutableListOf<Pair<EntityId, ComponentContainer>>()
        val candidates = state.zones
            .filterKeys { it.zoneType != Zone.SIDEBOARD }
            .values.flatten() + state.stack
        for (oldId in candidates.distinct()) {
            val container = state.getEntity(oldId) ?: continue
            if (container.has<VanguardAvatarComponent>()) {
                avatars += oldId to container
                continue
            }
            val card = container.get<CardComponent>() ?: continue
            if (container.has<TokenComponent>() || container.has<EmblemSourceComponent>()) continue
            // A copy with no card beneath it (a copied spell, a prepared copy, a copy made into a
            // collection) is not a Magic card, wherever it is.
            if (container.get<CopyOfComponent>()?.let { it.originalCardComponent == null } == true) continue
            val owner = container.get<OwnerComponent>()?.playerId ?: card.ownerId ?: continue
            if (owner !in involved) continue
            val zone = when {
                oldId in exemptInExile -> Zone.EXILE
                container.has<CommanderComponent>() -> Zone.COMMAND
                else -> Zone.LIBRARY
            }
            cards.getOrPut(owner) { mutableListOf() } += printedCard(container, card, owner) to zone
            oldIdsByOwner.getOrPut(owner) { mutableListOf() } += oldId
        }

        // Mint new ids per owner and hand them out in a shuffled order (as GameInitializer does).
        val idMap = mutableMapOf<EntityId, EntityId>()
        for ((owner, entries) in cards) {
            val newIds = entries.map {
                val (id, next) = fresh.newEntity()
                fresh = next
                id
            }
            val (shuffled, advanced) = fresh.nextRandom { shuffle(newIds) }
            fresh = advanced
            val oldIds = oldIdsByOwner.getValue(owner)
            entries.forEachIndexed { index, (container, zone) ->
                val newId = shuffled[index]
                idMap[oldIds[index]] = newId
                fresh = fresh.withEntity(newId, container).addToZone(ZoneKey(owner, zone), newId)
            }
        }

        for (playerId in state.turnOrder) {
            val old = state.getEntity(playerId) ?: continue
            fresh = fresh.withEntity(
                playerId,
                if (playerId in involved) newPlayer(old, playerId == starting, idMap) else old
            )
        }
        // Cards outside the game (CR 100.4) aren't involved in it and stay where they are, as they are.
        for ((key, ids) in state.zones) {
            if (key.zoneType != Zone.SIDEBOARD || key.ownerId !in involved) continue
            for (id in ids) {
                val container = state.getEntity(id) ?: continue
                fresh = fresh.withEntity(id, container).addToZone(key, id)
            }
        }
        for ((avatarId, container) in avatars) {
            val owner = container.get<VanguardAvatarComponent>()!!.ownerId
            if (owner !in involved) continue
            fresh = fresh.withEntity(avatarId, container).addToZone(ZoneKey(owner, Zone.COMMAND), avatarId)
        }

        val events = mutableListOf<GameEvent>(GameRestartedEvent(starting))
        for (playerId in involved) {
            val library = ZoneKey(playerId, Zone.LIBRARY)
            val (order, shuffledState) = fresh.nextRandom { shuffle(fresh.getZone(library)) }
            fresh = shuffledState.reorderZone(library, order)
            events += LibraryShuffledEvent(playerId, ShuffleCause.GAME_SETUP)
        }
        for (playerId in involved) {
            val (drawn, drawEvents) = GameInitializer.drawCards(fresh, playerId, MulliganStateComponent.STARTING_HAND_SIZE)
            fresh = drawn
            events += drawEvents
            // CR 727.3: a player whose deck can't supply seven cards loses when state-based actions
            // are first checked, in the first upkeep, whatever mulligans they take.
            if (drawEvents.any { it is DrawFailedEvent }) {
                fresh = fresh.updateEntity(playerId) { it.with(AttemptedDrawFromEmptyLibraryComponent) }
            }
        }

        fresh = fresh.copy(restartFollowUp = request.followUp?.let { remap(it, idMap) })
        val mulligansSkipped = involved.all { fresh.getEntity(it)?.get<MulliganStateComponent>()?.skipped == true }
        if (mulligansSkipped && beginFirstTurn != null) return beginFirstTurn.invoke(fresh, events)
        return ExecutionResult.success(fresh, events)
    }

    /**
     * The card as printed, owned and controlled by [owner]: rebuilt from its definition, so no
     * counter, designation, copy effect, face or other state from the old game survives. The art it
     * was played with is kept. A card whose definition can't be found keeps its printed component.
     */
    private fun printedCard(container: ComponentContainer, current: CardComponent, owner: EntityId): ComponentContainer {
        val uncopied = container.get<CopyOfComponent>()?.originalCardComponent ?: current
        val dfc = container.get<DoubleFacedComponent>()
        val printed = if (dfc != null && dfc.isBack) dfc.frontFaceCard ?: uncopied else uncopied
        val definition = cardRegistry.getCard(printed.cardDefinitionId)
        val rebuilt = if (definition == null) {
            ComponentContainer.of(printed.copy(ownerId = owner), OwnerComponent(owner), ControllerComponent(owner))
        } else {
            val made = CardEntityFactory.create(definition, owner)
            val card = made.get<CardComponent>()!!
            made.with(card.copy(
                imageUri = printed.imageUri,
                backFaceImageUri = printed.backFaceImageUri,
                printingSetCode = printed.printingSetCode,
            ))
        }
        // Commander tax counts casts in this game only (CR 903.8).
        return container.get<CommanderComponent>()?.let { rebuilt.with(CommanderComponent(ownerId = it.ownerId)) } ?: rebuilt
    }

    /**
     * A player at the start of a game: starting life, empty pool, no history, mulligans ahead — or
     * none, when the game was set up without them. What the session set up around the game rather
     * than inside it (team, hotseat control) stays.
     */
    private fun newPlayer(old: ComponentContainer, isStarting: Boolean, idMap: Map<EntityId, EntityId>): ComponentContainer {
        val player = old.get<PlayerComponent>()!!
        val mulligans = old.get<MulliganStateComponent>()
        val skipMulligans = mulligans?.skipped == true
        var container = ComponentContainer.of(
            player,
            LifeTotalComponent(player.startingLifeTotal),
            ManaPoolComponent(),
            LandDropsComponent(),
            // As at setup, the starting player's first turn is counted as it begins.
            PlayerTurnsTakenComponent(count = if (isStarting) 1 else 0),
            MulliganStateComponent(
                hasKept = skipMulligans,
                freeMulligan = mulligans?.freeMulligan == true,
                skipped = skipMulligans,
            ),
        )
        old.get<TeamComponent>()?.let { container = container.with(it) }
        old.get<HotseatControlComponent>()?.let { container = container.with(it) }
        old.get<CommanderRegistryComponent>()?.let { registry ->
            container = container.with(CommanderRegistryComponent(registry.commanderIds.mapNotNull { idMap[it] }))
        }
        return container
    }

    /**
     * The turn order beginning with [starting]. Shared team turns (CR 805) seat teammates together
     * and the team takes the turn, so the order begins at the first seat of [starting]'s team.
     */
    private fun startingFrom(state: GameState, starting: EntityId): List<EntityId> {
        val order = state.turnOrder
        var index = order.indexOf(starting)
        if (state.format.sharesTeamTurns) {
            val team = state.teamOf(starting)
            repeat(order.size - 1) {
                val previous = (index - 1 + order.size) % order.size
                if (state.teamOf(order[previous]) == team) index = previous
            }
        }
        return order.subList(index, order.size) + order.subList(0, index)
    }

    private fun remap(followUp: TurnStartFollowUp, idMap: Map<EntityId, EntityId>): TurnStartFollowUp {
        val context = followUp.context
        return followUp.copy(context = context.copy(
            sourceId = context.sourceId?.let { idMap[it] },
            pipeline = PipelineState(
                storedCollections = context.pipeline.storedCollections.mapValues { (_, ids) -> ids.mapNotNull { idMap[it] } }
            ),
        ))
    }
}
