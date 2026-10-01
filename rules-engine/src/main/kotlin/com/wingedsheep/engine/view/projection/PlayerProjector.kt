package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.view.ClientCommanderDamage
import com.wingedsheep.engine.view.ClientManaPool
import com.wingedsheep.engine.view.ClientPlayer
import com.wingedsheep.engine.view.ClientRestrictedManaEntry
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry

/**
 * Projects a player — life, poison, zone sizes, mana pool, commander damage, team info — and the
 * badges on them (delegated to [PlayerActiveEffectsProjector]). All of it is public information.
 */
internal class PlayerProjector(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator,
    private val activeEffects: PlayerActiveEffectsProjector,
) {
    // Reused (with conditionEvaluator + cardRegistry) to surface each player's effective maximum
    // hand size via the shared com.wingedsheep.engine.core.MaximumHandSize source of truth.
    private val dynamicAmountEvaluator = conditionEvaluator.amounts

    fun project(state: GameState, playerId: EntityId): ClientPlayer {
        val container = state.getEntity(playerId)
        val playerComponent = container?.get<PlayerComponent>()
        // CR 810.9a — a player's displayed life is the team's shared total in Two-Headed Giant.
        val displayedLife = if (container?.get<LifeTotalComponent>() != null) state.lifeTotal(playerId) else null
        val landDropsComponent = container?.get<LandDropsComponent>()

        // Effective maximum hand size (CR 402.2): 7 by default, smaller/larger when an effect set
        // it, null when unlimited (Reliquary Tower). Shared source of truth with cleanup.
        val maxHandSize = MaximumHandSize.effective(
            state, playerId, cardRegistry, conditionEvaluator, dynamicAmountEvaluator
        )

        // Determine lands played this turn
        val landsPlayed = if (landDropsComponent != null) {
            landDropsComponent.maxPerTurn - landDropsComponent.remaining
        } else {
            0
        }

        // A player has lost when the engine has marked them (mid-game elimination in a
        // multiplayer pod — drives the opponent-rail tombstone while the game continues),
        // or at game end when someone else won (2-player degenerate case).
        val hasLost = container?.has<PlayerLostComponent>() == true ||
            (state.gameOver && state.winnerId != null && state.winnerId != playerId)

        return ClientPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = displayedLife ?: 20,
            // CR 810.10a — a player's displayed poison is the team's pooled total in Two-Headed
            // Giant, the same way [life] above is the team's shared total. The team orb and the
            // 15-counter loss check (CR 810.8d) must read the same number.
            poisonCounters = state.teamPoison(playerId),
            handSize = state.getHand(playerId).size,
            maxHandSize = maxHandSize,
            librarySize = state.getLibrary(playerId).size,
            graveyardSize = state.getGraveyard(playerId).size,
            exileSize = state.getExile(playerId).size,
            landsPlayedThisTurn = landsPlayed,
            hasLost = hasLost,
            // Mana pool is public information in MTG - show for all players
            manaPool = container?.get<ManaPoolComponent>()?.let(::manaPool),
            manaPaymentColors = com.wingedsheep.engine.mechanics.mana.ManaSpendingRules.colors(state, playerId)
                .mapKeys { it.key.symbol.toString() }.mapValues { (_, colors) -> colors.map { it.symbol.toString() } },
            activeEffects = activeEffects.project(state, playerId, container),
            commanderDamage = commanderDamage(state, playerId),
            // CR 702.179 — public information, and 0 for the overwhelming majority of games.
            speed = state.speed(playerId),
            // CR 107.14 — public information like poison counters, and 0 outside energy decks.
            energyCounters = container?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0,
            // Team variants (CR 810 / CR 808). Public information, and absent from every
            // non-team game, so both fields serialize away by default.
            teamIndex = container?.get<TeamComponent>()?.teamIndex,
            teamSharedLife = state.format.sharesTeamLife,
            teamSharedTurns = state.format.sharesTeamTurns
        )
    }

    private fun manaPool(manaPoolComponent: ManaPoolComponent): ClientManaPool =
        ClientManaPool(
            white = manaPoolComponent.white,
            blue = manaPoolComponent.blue,
            black = manaPoolComponent.black,
            red = manaPoolComponent.red,
            green = manaPoolComponent.green,
            colorless = manaPoolComponent.colorless,
            restrictedMana = manaPoolComponent.restrictedMana.map { entry ->
                val expiryNote = when (entry.expiry) {
                    ManaExpiry.END_OF_COMBAT -> "This mana lasts until end of combat, then is lost."
                    ManaExpiry.KEPT_UNTIL_END_OF_TURN -> "This mana lasts until end of turn."
                    ManaExpiry.END_OF_TURN -> null
                }
                ClientRestrictedManaEntry(
                    color = entry.color?.symbol?.toString(),
                    restrictionDescription = listOfNotNull(
                        entry.restriction.description.ifBlank { null },
                        expiryNote
                    ).joinToString(" ")
                )
            }
        )

    /**
     * Build per-commander damage tallies against [playerId]. Empty when the format has no commanders
     * and for defenders no commander has connected with yet.
     */
    private fun commanderDamage(
        state: GameState,
        playerId: EntityId
    ): List<ClientCommanderDamage> {
        val threshold = state.format.commanderDamageThreshold ?: return emptyList()
        if (state.commanderDamage.isEmpty()) return emptyList()

        return state.commanderDamage
            .asSequence()
            .filter { it.defendingPlayerId == playerId && it.amount > 0 }
            .mapNotNull { entry ->
                val container = state.getEntity(entry.commanderId) ?: return@mapNotNull null
                val card = container.get<CardComponent>() ?: return@mapNotNull null
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: card.ownerId
                    ?: return@mapNotNull null
                ClientCommanderDamage(
                    commanderId = entry.commanderId,
                    commanderName = card.name,
                    controllerId = controllerId,
                    amount = entry.amount,
                    threshold = threshold,
                    imageUri = card.imageUri,
                )
            }
            .sortedByDescending { it.amount }
            .toList()
    }
}
