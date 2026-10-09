package com.wingedsheep.ai.engine

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * What a "choose a colour / land type / creature type / card name" answer should lean towards when
 * simulating the options can't tell them apart.
 *
 * A one-step simulation is blind to most of these choices. The colour a Room of Refuge taps for,
 * the landwalk a Traveler's Cloak grants and the name a Lammastide Weave guesses all leave the
 * board identical at the moment of choosing — they pay off on a later turn, at a later block, or
 * off a card nobody has seen yet. Every option then scores the same and `maxByOrNull` returns the
 * first one, which is how an R/G deck came to tap a land for white for the rest of a game. These
 * priors are the tie-break, never a replacement for the simulation: an option the board *can*
 * see as better still wins on its score.
 *
 * Each prior has a **polarity**. A choice that feeds the chooser — the colour its own land adds,
 * the creature type its own lord pumps — should lean towards the chooser's deck; a choice that is
 * a weapon — protection, landwalk, a hoser, a name to strip or mill — towards the opponents'. The
 * polarity is read off the choice's source: a permanent with a mana ability is choosing for its
 * controller's mana, everything else defaults to the per-kind lean documented on each method.
 *
 * Information: the priors read the multiset of cards each player owns in hand and library, never
 * their order or which card sits where. That is the same knowledge
 * [com.wingedsheep.ai.engine.hidden.OpponentModel.IdentityPermutation] grants the search, and less
 * than a known decklist, so it introduces nothing the AI did not already have.
 */
internal class ChoicePriors(private val cardRegistry: CardRegistry) {

    enum class Polarity { OWN, OPPONENT }

    /**
     * [Polarity.OWN] when the choice's source is a permanent that makes mana — the colour or land
     * type it is choosing is the colour its controller will be tapping it for. [fallback] otherwise.
     */
    fun polarityOf(state: GameState, sourceId: EntityId?, fallback: Polarity): Polarity {
        val card = sourceId?.let { state.getEntity(it)?.get<CardComponent>() } ?: return fallback
        val definition = cardRegistry.getCard(card.cardDefinitionId) ?: return fallback
        val makesMana = definition.script.activatedAbilities.any { it.isManaAbility }
        return if (makesMana) Polarity.OWN else fallback
    }

    /**
     * How much each colour matters to [playerId] ([Polarity.OWN]) or to its opponents.
     *
     * Own: coloured pips still to be paid — every card in hand and library — divided by the basic
     * lands already producing that colour, so a deck short of its second colour leans to it.
     * Opponent: one point per opposing card of that colour, double for a permanent already on the
     * battlefield, which is the thing protection or a hoser is usually asked to answer now.
     */
    fun colorWeights(state: GameState, playerId: EntityId, polarity: Polarity): Map<Color, Double> =
        when (polarity) {
            Polarity.OWN -> ownColorDemand(state, playerId)
            Polarity.OPPONENT -> {
                val projected = state.projectedState
                val weights = HashMap<Color, Double>()
                for (opponent in state.getOpponents(playerId)) {
                    for (id in state.controlledBattlefield(opponent)) {
                        for (name in projected.getColors(id)) {
                            val color = Color.entries.firstOrNull { it.name == name } ?: continue
                            weights.merge(color, 2.0, Double::plus)
                        }
                    }
                    for (card in hiddenCards(state, opponent)) {
                        card.colors.forEach { weights.merge(it, 1.0, Double::plus) }
                    }
                }
                weights
            }
        }

    /**
     * How much each basic land type matters. Own: the colour that type taps for, weighted as in
     * [colorWeights]. Opponent: landwalk and "lands of the chosen type" answers are about the lands
     * the opponent *has*, so each land on their battlefield counts one, and their deck adds only
     * a fraction — enough to order types the battlefield leaves tied, never to outvote it.
     */
    fun landTypeWeights(state: GameState, playerId: EntityId, polarity: Polarity): Map<String, Double> =
        when (polarity) {
            Polarity.OWN -> {
                val demand = ownColorDemand(state, playerId)
                BASIC_LAND_COLORS.mapValues { (_, color) -> demand[color] ?: 0.0 }
            }
            Polarity.OPPONENT -> {
                val projected = state.projectedState
                val weights = HashMap<String, Double>()
                for (opponent in state.getOpponents(playerId)) {
                    for (id in state.controlledBattlefield(opponent)) {
                        for (subtype in projected.getSubtypes(id)) {
                            val type = BASIC_LAND_COLORS.keys.firstOrNull { it.equals(subtype, ignoreCase = true) }
                                ?: continue
                            weights.merge(type, 1.0, Double::plus)
                        }
                    }
                    // The deck's share stays below one whole land so it can only order types the
                    // battlefield leaves tied, however many basics the library holds.
                    val inDeck = hiddenCards(state, opponent).flatMap { card ->
                        card.typeLine.subtypes.mapNotNull { subtype ->
                            BASIC_LAND_COLORS.keys.firstOrNull { it.equals(subtype.value, ignoreCase = true) }
                        }
                    }
                    inDeck.groupingBy { it }.eachCount().forEach { (type, count) ->
                        weights.merge(type, count / (inDeck.size + 1.0), Double::plus)
                    }
                }
                weights
            }
        }

    /**
     * Card names [players] still have to play with, each copy weighted by its mana value plus one.
     *
     * A name is worth naming in proportion to how much of the game it still is: the copies in hand,
     * library and on the battlefield — not the graveyard, which is spent. Mana value is the payoff
     * scale most naming cards share (Lammastide Weave gains it as life; a Spyglass or a Needle
     * shuts off the card the opponent paid most for), and the +1 keeps a pile of lands from
     * weighing nothing.
     */
    fun nameWeights(state: GameState, players: List<EntityId>): Map<String, Double> {
        val weights = HashMap<String, Double>()
        for (player in players) {
            for (card in hiddenCards(state, player) + battlefieldCards(state, player)) {
                weights.merge(card.name, card.manaCost.cmc + 1.0, Double::plus)
            }
        }
        return weights
    }

    /** Creature types among the creature cards [players] still have to play with, one point per card. */
    fun creatureTypeWeights(state: GameState, players: List<EntityId>): Map<String, Double> {
        val projected = state.projectedState
        val weights = HashMap<String, Double>()
        for (player in players) {
            for (id in state.controlledBattlefield(player)) {
                if (!projected.isCreature(id)) continue
                projected.getSubtypes(id).forEach { weights.merge(it.lowercase(), 1.0, Double::plus) }
            }
            for (card in hiddenCards(state, player)) {
                if (!card.typeLine.isCreature) continue
                card.typeLine.subtypes.forEach { weights.merge(it.value.lowercase(), 1.0, Double::plus) }
            }
        }
        return weights
    }

    private fun ownColorDemand(state: GameState, playerId: EntityId): Map<Color, Double> {
        val demand = HashMap<Color, Double>()
        for (card in hiddenCards(state, playerId)) {
            card.manaCost.colorCount.forEach { (color, pips) -> demand.merge(color, pips.toDouble(), Double::plus) }
        }
        val projected = state.projectedState
        val supply = HashMap<Color, Int>()
        for (id in state.controlledBattlefield(playerId)) {
            for (subtype in projected.getSubtypes(id)) {
                val color = BASIC_LAND_COLORS.entries.firstOrNull { it.key.equals(subtype, ignoreCase = true) }?.value
                    ?: continue
                supply.merge(color, 1, Int::plus)
            }
        }
        return demand.mapValues { (color, pips) -> pips / (1.0 + (supply[color] ?: 0)) }
    }

    /** Cards in hand and library — what a player still has to draw or cast. Read as a multiset. */
    private fun hiddenCards(state: GameState, playerId: EntityId): List<CardComponent> =
        (state.getZone(playerId, Zone.HAND) + state.getZone(playerId, Zone.LIBRARY))
            .mapNotNull { state.getEntity(it)?.get<CardComponent>() }

    private fun battlefieldCards(state: GameState, playerId: EntityId): List<CardComponent> =
        state.controlledBattlefield(playerId).mapNotNull { state.getEntity(it)?.get<CardComponent>() }

    companion object {
        val BASIC_LAND_COLORS: Map<String, Color> = mapOf(
            "Plains" to Color.WHITE,
            "Island" to Color.BLUE,
            "Swamp" to Color.BLACK,
            "Mountain" to Color.RED,
            "Forest" to Color.GREEN,
        )

        init {
            check(BASIC_LAND_COLORS.keys == Subtype.ALL_BASIC_LAND_TYPES) {
                "Every basic land type needs the colour it taps for"
            }
        }
    }
}
