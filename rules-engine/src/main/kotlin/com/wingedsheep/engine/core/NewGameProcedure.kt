package com.wingedsheep.engine.core

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
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.VanguardAvatarComponent
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * The steps shared by every game that begins from cards already in play rather than from deck
 * lists: a restarted game (CR 727, [GameRestarter]) and a subgame (CR 729, [Subgames]). The cards
 * become their printed selves under fresh ids, the players start over, and opening hands are dealt
 * by the CR 103 procedure.
 */
internal class NewGameProcedure(private val cardRegistry: CardRegistry) {

    /** A Magic card in a game, under the id it has there. */
    data class GameCard(val id: EntityId, val container: ComponentContainer, val owner: EntityId)

    /**
     * An empty game carrying [from]'s settings and the counters that must keep running across games
     * — the random generator and the id, routing and object-generation allocators — at turn 1 with
     * [turnOrder]'s first player active.
     */
    fun emptyGame(from: GameState, turnOrder: List<EntityId>): GameState = GameState(
        preserveGraveyardOrder = from.preserveGraveyardOrder,
        format = from.format,
        attackMode = from.attackMode,
        chaosRules = from.chaosRules,
        rng = from.rng,
        nextEntityId = from.nextEntityId,
        nextRoutingId = from.nextRoutingId,
        nextObjectGeneration = from.nextObjectGeneration,
        timestamp = from.timestamp,
        yieldsByPlayer = from.yieldsByPlayer,
        manualTriggerOrdering = from.manualTriggerOrdering,
    ).clearUntilEndOfTurnYields().copy(
        turnOrder = turnOrder,
        activePlayerId = turnOrder.first(),
        priorityPlayerId = turnOrder.first(),
        turnNumber = 1,
        roundNumber = 1,
        phase = Phase.BEGINNING,
        step = Step.UNTAP,
    )

    /**
     * Every Magic card [owners] own in [state]'s game — any zone but the sideboard, phased out or
     * not, cast or not, and both halves of a melded or merged permanent — in a stable order. Tokens,
     * copies of cards with no card beneath them, emblems and vanguard avatars aren't Magic cards of
     * the game and are left out.
     */
    fun cardsInGame(state: GameState, owners: Set<EntityId>): List<GameCard> {
        // A melded permanent is two cards (CR 712.21); the second one sits in no zone.
        // A merged (mutated) permanent is one card per component (CR 730.3), the others zoneless too.
        val meldPartners = state.getBattlefield().flatMap { id ->
            val container = state.getEntity(id)
            listOfNotNull(container?.get<MeldedComponent>()?.partnerId) +
                container?.get<com.wingedsheep.engine.mechanics.MergedComponent>()?.partnerIds(id).orEmpty()
        }
        val candidates = state.zones
            .filterKeys { it.zoneType != Zone.SIDEBOARD }
            .values.flatten() + state.stack + meldPartners
        return candidates.distinct().mapNotNull { id ->
            val container = state.getEntity(id) ?: return@mapNotNull null
            if (container.has<VanguardAvatarComponent>()) return@mapNotNull null
            val card = container.get<CardComponent>() ?: return@mapNotNull null
            if (container.has<TokenComponent>() || container.has<EmblemSourceComponent>()) return@mapNotNull null
            // A copy with no card beneath it (a copied spell, a prepared copy, a copy made into a
            // collection) is not a Magic card, wherever it is.
            if (container.get<CopyOfComponent>()?.let { it.originalCardComponent == null } == true) return@mapNotNull null
            val owner = container.get<OwnerComponent>()?.playerId ?: card.ownerId ?: return@mapNotNull null
            if (owner !in owners) return@mapNotNull null
            GameCard(id, container, owner)
        }
    }

    /**
     * The card as printed, owned and controlled by [owner]: rebuilt from its definition, so no
     * counter, designation, copy effect, face or other state from the game it left survives. The
     * art it was played with is kept. A card whose definition can't be found keeps its printed
     * component.
     */
    fun printedCard(container: ComponentContainer, owner: EntityId): ComponentContainer {
        val current = container.get<CardComponent>()!!
        val uncopied = container.get<MeldedComponent>()?.hostFrontCard
            ?: container.get<com.wingedsheep.engine.mechanics.MergedComponent>()?.hostCard
            ?: container.get<CopyOfComponent>()?.originalCardComponent ?: current
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
     * Put [cards] (one owner's, each with the zone it goes to) into [state] under new entity ids,
     * minted per owner and handed out in a shuffled order (as [GameInitializer] does), so an id one
     * game made public can't name a card in another game's library or hand. Returns the old-to-new
     * id map alongside the state.
     */
    fun placeUnderFreshIds(
        state: GameState,
        owner: EntityId,
        cards: List<Triple<EntityId, ComponentContainer, Zone>>,
    ): Pair<GameState, Map<EntityId, EntityId>> {
        var current = state
        val newIds = cards.map {
            val (id, next) = current.newEntity()
            current = next
            id
        }
        val (shuffled, advanced) = current.nextRandom { shuffle(newIds) }
        current = advanced
        val idMap = mutableMapOf<EntityId, EntityId>()
        cards.forEachIndexed { index, (oldId, container, zone) ->
            val newId = shuffled[index]
            idMap[oldId] = newId
            current = current.withEntity(newId, container).addToZone(ZoneKey(owner, zone), newId)
        }
        return current to idMap
    }

    /**
     * A player at the start of a game: starting life, empty pool, no history, mulligans ahead — or
     * none, when the game was set up without them. What the session set up around the game rather
     * than inside it (team, hotseat control) stays. [idMap] renames the player's commanders.
     */
    fun newPlayer(old: ComponentContainer, isStarting: Boolean, idMap: Map<EntityId, EntityId>): ComponentContainer {
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
        // A companion still waiting outside the game stays there, revealed, under the same id —
        // sideboard cards aren't part of the new game. One already put into hand is now just a
        // card of the deck, so the new game has no companion.
        old.get<com.wingedsheep.engine.state.components.player.CompanionComponent>()
            ?.takeIf { !it.putIntoHand }
            ?.let { container = container.with(it) }
        return container
    }

    /**
     * [state]'s turn order, restricted to [players] and beginning with [starting]. Shared team
     * turns (CR 805) seat teammates together and the team takes the turn, so the order begins at
     * the first seat of [starting]'s team.
     */
    fun turnOrderFrom(state: GameState, players: Set<EntityId>, starting: EntityId): List<EntityId> {
        val order = state.turnOrder.filter { it in players }
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

    /**
     * Shuffle each of [players]' libraries and draw their opening hands. A player whose deck can't
     * supply seven cards loses when state-based actions are first checked, in the first upkeep,
     * whatever mulligans they take (CR 727.3, 729.3).
     */
    fun dealOpeningHands(state: GameState, players: List<EntityId>): Pair<GameState, List<GameEvent>> {
        var current = state
        val events = mutableListOf<GameEvent>()
        for (playerId in players) {
            val library = ZoneKey(playerId, Zone.LIBRARY)
            val (order, shuffled) = current.nextRandom { shuffle(current.getZone(library)) }
            current = shuffled.reorderZone(library, order)
            events += LibraryShuffledEvent(playerId, ShuffleCause.GAME_SETUP)
        }
        for (playerId in players) {
            val (drawn, drawEvents) = GameInitializer.drawCards(current, playerId, MulliganStateComponent.STARTING_HAND_SIZE)
            current = drawn
            events += drawEvents
            if (drawEvents.any { it is DrawFailedEvent }) {
                current = current.updateEntity(playerId) { it.with(AttemptedDrawFromEmptyLibraryComponent) }
            }
        }
        return current to events
    }

    /** Every player in [state] has its mulligans skipped: the game was set up without them. */
    fun mulligansSkipped(state: GameState, players: List<EntityId>): Boolean =
        players.all { state.getEntity(it)?.get<MulliganStateComponent>()?.skipped == true }
}
