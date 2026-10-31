package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * What a card is worth to keep *in this position*, priced by when it can actually be cast — the
 * value behind every "which card do I keep / take / discard" answer once
 * [AiProfile.castabilityAwareCardSelection] is on.
 *
 * The legacy scorer in [DecisionResponder] knew one fact about mana: how many lands are on the
 * battlefield. That left it blind in four ways the game logs caught it out on:
 *  - **Lands in hand did not exist.** A land was priced off the battlefield count alone, so with a
 *    Plains already waiting in hand the AI still took a second land over a removal spell
 *    (Orazca Puzzle-Door), and a land was "desperately needed" at two lands even with three more
 *    in hand.
 *  - **The land drop did not exist.** The one land in hand while the drop is still unspent is mana
 *    *this turn*; pitching it to a connive costs a whole turn of development.
 *  - **Delay was nearly free, and "no creatures yet" outweighed it.** A 7-drop at one land lost
 *    3.0 for being uncastable and gained 5.0 for being a creature on an empty board, so a 1-land
 *    hand discarded its three-mana spell and kept its seven-drop.
 *  - **Colour did not exist.** With only Swamps out it kept white cards it had no source for and
 *    discarded black ones it could cast as soon as the next land arrived.
 *
 * So this prices a spell as its intrinsic worth minus [DELAY_PER_TURN] for every turn the player
 * is expected to wait before casting it: lands in hand count one turn each, lands still to be
 * drawn cost the expected draws to find one (read off the player's own library, whose *contents*
 * a player knows from their decklist even though the order is hidden), and each coloured pip no
 * source can pay costs the expected draws to find a land of that colour. A land is priced by how
 * much mana the player already has coming, plus what it unlocks — this turn's drop, a hand of
 * spells above the mana curve, a colour nothing else produces.
 */
internal class CardSelectionValue private constructor(
    private val state: GameState,
    private val playerId: EntityId,
    private val intents: IntentCatalog,
    /** Lands on the battlefield under this player's control. */
    private val landsInPlay: Int,
    /** Colours of each mana source in play, one entry per land. */
    private val inPlayColors: List<Set<Color>>,
    /** Land cards in hand (entity, the colours it produces). */
    private val handLands: List<Pair<EntityId, Set<Color>>>,
    /** Nonland cards in hand. */
    private val handSpells: List<Pair<EntityId, CardComponent>>,
    /** Whether a land from hand can still be played this turn. */
    private val landDropNow: Boolean,
    /**
     * The cards being priced stay in the library to be drawn on a later turn — scry, surveil and
     * library reorders — so none of them can fill this turn's land drop.
     */
    private val drawnLater: Boolean,
    private val librarySize: Int,
    private val libraryLands: Int,
    private val libraryLandsByColor: Map<Color, Int>,
    /** Legendary permanents this player controls, by name. */
    private val legendsInPlay: Map<String, List<EntityId>>,
    /** What each turn of waiting to cast a spell costs it — see [of]'s `denial`. */
    private val delayPerTurn: Double,
) {

    /** Higher = more valuable to keep. */
    fun score(cardId: EntityId): Double {
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return 0.0
        return if (card.isLand) landScore(cardId, card) else spellScore(cardId, card)
    }

    /**
     * Short of lands: some spell in hand costs more than the lands in play and in hand add up to,
     * so the next land drawn is mana a held card is waiting on. The same reach
     * `CardAdvantage` reads to tell a land in hand worth a card from a surplus one
     * ([AiProfile.surplusLandsReadTheHand]).
     */
    val landShort: Boolean
        get() = handSpells.any { it.second.manaValue > landsInPlay + handLands.size }

    /** The mean [score] of the player's library cards — what a random draw is worth; 0 when it is empty. */
    fun averageDraw(): Double {
        val library = state.getZone(playerId, Zone.LIBRARY)
        return if (library.isEmpty()) 0.0 else library.sumOf { score(it) } / library.size
    }

    /** Turns until [card] is castable on mana alone, given what is coming. Exposed for tests. */
    internal fun turnsUntilCastable(cardId: EntityId, card: CardComponent): Double {
        val otherHandLands = handLands.filter { it.first != cardId }
        val shortfall = (card.manaValue - landsInPlay).coerceAtLeast(0)
        val fromHand = minOf(shortfall, otherHandLands.size)
        // The first land from hand arrives this turn if the drop is unspent.
        val handTurns = if (fromHand > 0 && landDropNow) fromHand - 1 else fromHand
        val genericTurns = handTurns + (shortfall - fromHand) * turnsToDraw(libraryLands)

        val available = inPlayColors + otherHandLands.map { it.second }
        var colorTurns = 0.0
        for ((color, needed) in card.manaCost.colorCount) {
            val have = available.count { color in it }
            val missing = (needed - have).coerceAtLeast(0)
            colorTurns += missing * turnsToDraw(libraryLandsByColor[color] ?: 0)
        }
        // A land of the missing colour is also a land toward the generic part, so the two waits
        // overlap rather than add.
        return maxOf(genericTurns.toDouble(), colorTurns)
    }

    private fun turnsToDraw(sources: Int): Double =
        if (sources <= 0 || librarySize <= 0) NEVER_TURNS
        else (librarySize.toDouble() / sources).coerceIn(1.0, NEVER_TURNS)

    private fun spellScore(cardId: EntityId, card: CardComponent): Double {
        val turns = turnsUntilCastable(cardId, card)
        var score = SPELL_BASE + card.manaValue * MANA_VALUE_WEIGHT

        if (card.isCreature) {
            score += 2.0
            // The first creature matters — but only one that will actually arrive soon. Granting
            // this to an uncastable seven-drop is what made a one-land hand keep it.
            if (turns <= 1.0 && !controlsCreature()) score += 3.0
            val keywords = card.baseKeywords
            if (Keyword.FLYING in keywords) score += 1.0
            if (Keyword.DEATHTOUCH in keywords) score += 1.0
            if (Keyword.LIFELINK in keywords) score += 0.5
        }
        if (card.typeLine.isInstant) score += 2.5
        if (card.typeLine.isSorcery) score += 2.0

        intents.forName(card.name)?.let { intent ->
            if (REMOVAL_TAGS.any { it in intent }) score += REMOVAL_BONUS
            else if (IntentTag.DRAW in intent) score += 0.5
        }

        // A second copy of a legend is a dead card while the first is (or will be) out.
        if (card.typeLine.isLegendary && isRedundantLegend(cardId, card.name)) score -= LEGEND_DUPLICATE_PENALTY

        return score - turns * delayPerTurn
    }

    private fun landScore(cardId: EntityId, card: CardComponent): Double {
        val otherHandLands = handLands.filter { it.first != cardId }
        val coming = landsInPlay + otherHandLands.size
        var score = when {
            coming <= 1 -> 9.0
            coming == 2 -> 8.0
            coming == 3 -> 6.0
            coming == 4 -> 4.5
            coming == 5 -> 3.0
            coming == 6 -> 1.5
            else -> 0.5
        }
        // The only land for an unspent drop is mana this very turn — but only a card in hand can be
        // that land. One left on top of the library is drawn next turn at the earliest, by which
        // time the drop it would fill is a fresh one the base curve already prices; counting this
        // turn's drop kept a Plains on top of a thirteen-land board (0.5 + 1.5 = the keep line).
        if (landDropNow && !drawnLater && otherHandLands.isEmpty()) score += 1.5

        // Spells in hand above the mana that is already coming want another land.
        val topEnd = handSpells.maxOfOrNull { it.second.manaValue } ?: 0
        score += (topEnd - coming).coerceIn(0, 3) * 0.75

        // A colour nothing else provides, which spells in hand are waiting on.
        val produces = landColors(card)
        if (produces.isNotEmpty()) {
            val available = inPlayColors + otherHandLands.map { it.second }
            val unlocked = handSpells.count { (_, spell) ->
                spell.manaCost.colorCount.keys.any { color -> color in produces && available.none { color in it } }
            }
            score += minOf(unlocked, 3) * 1.0
        }
        return score
    }

    private fun controlsCreature(): Boolean =
        state.projectedState.getBattlefieldControlledBy(playerId).any { state.projectedState.isCreature(it) }

    private fun isRedundantLegend(cardId: EntityId, name: String): Boolean {
        // The scored card itself on the battlefield (a top-or-bottom choice) is no duplicate of itself.
        if (legendsInPlay[name].orEmpty().any { it != cardId }) return true
        // Among copies in hand, the first one keeps its value; the later ones are the spares.
        val copies = handSpells.filter { it.second.name == name }.map { it.first }
        val index = copies.indexOf(cardId)
        return if (index >= 0) index > 0 else copies.isNotEmpty()
    }

    companion object {
        /** The wait, in turns, charged for a card nothing in the library can unlock. */
        private const val NEVER_TURNS = 10.0
        private const val DELAY_PER_TURN = 0.7

        /**
         * The wait's price when the card is being taken *away* from its holder: a card the
         * opponent casts in two turns still lands on us, so delay only halves its threat, and a
         * card no source can ever pay for ([NEVER_TURNS]) still prices near nothing.
         */
        private const val DENIAL_DELAY_PER_TURN = 0.35
        private const val SPELL_BASE = 3.0
        private const val MANA_VALUE_WEIGHT = 0.3
        private const val REMOVAL_BONUS = 2.0
        private const val LEGEND_DUPLICATE_PENALTY = 2.5
        private val REMOVAL_TAGS = listOf(
            IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.FIGHT, IntentTag.NEUTRALIZE,
        )

        private val BASIC_COLORS = mapOf(
            Subtype.PLAINS to Color.WHITE,
            Subtype.ISLAND to Color.BLUE,
            Subtype.SWAMP to Color.BLACK,
            Subtype.MOUNTAIN to Color.RED,
            Subtype.FOREST to Color.GREEN,
        )
        private val MANA_SYMBOL = Regex("""\{([^}]*[WUBRG][^}]*)\}""")

        /**
         * The colours a land card can make: its basic land types (CR 305.6) plus the coloured
         * mana symbols in its rules text. The same reading CR 903.4 uses for colour identity, and
         * close enough for a land — a fetch land reads as colourless, which only undervalues it.
         */
        fun landColors(card: CardComponent): Set<Color> {
            val colors = mutableSetOf<Color>()
            card.typeLine.subtypes.forEach { BASIC_COLORS[it]?.let(colors::add) }
            MANA_SYMBOL.findAll(card.oracleText).forEach { match ->
                match.groupValues[1].forEach { ch -> Color.fromSymbol(ch)?.let(colors::add) }
            }
            return colors
        }

        /**
         * [drawnLater]: the priced cards stay in the library for a later draw (scry, surveil,
         * library reorder) rather than reaching the hand now, so this turn's land drop is no
         * reason to keep a land.
         *
         * [denial]: the score is read for a choice that takes the card away from [playerId] — an
         * opponent's discard or exile chosen by someone else — so a spell's wait costs it
         * [DENIAL_DELAY_PER_TURN] instead of [DELAY_PER_TURN]. The holder's tempo is not the
         * chooser's concern; what the card does once it is cast is.
         */
        fun of(
            state: GameState,
            playerId: EntityId,
            intents: IntentCatalog,
            drawnLater: Boolean = false,
            denial: Boolean = false,
        ): CardSelectionValue {
            val projected = state.projectedState
            val myPermanents = projected.getBattlefieldControlledBy(playerId)
            val landsInPlay = myPermanents.filter { projected.hasType(it, "LAND") }
            val inPlayColors = landsInPlay.map { id ->
                val fromSubtypes = projected.getSubtypes(id).mapNotNull { BASIC_COLORS[Subtype(it)] }
                val printed = state.getEntity(id)?.get<CardComponent>()?.let(::landColors) ?: emptySet()
                fromSubtypes.toSet() + printed
            }

            val hand = state.getZone(playerId, Zone.HAND)
                .mapNotNull { id -> state.getEntity(id)?.get<CardComponent>()?.let { id to it } }
            val handLands = hand.filter { it.second.isLand }.map { it.first to landColors(it.second) }
            val handSpells = hand.filter { !it.second.isLand }

            val drops = state.getEntity(playerId)?.get<LandDropsComponent>()?.remaining ?: 0
            val landDropNow = state.activePlayerId == playerId && drops > 0 &&
                state.step.ordinal <= Step.POSTCOMBAT_MAIN.ordinal

            val library = state.getZone(playerId, Zone.LIBRARY)
                .mapNotNull { state.getEntity(it)?.get<CardComponent>() }
            val libraryLands = library.filter { it.isLand }
            val byColor = Color.entries.associateWith { color ->
                libraryLands.count { color in landColors(it) }
            }

            val legends = myPermanents.mapNotNull { id ->
                state.getEntity(id)?.get<CardComponent>()?.takeIf { it.typeLine.isLegendary }?.let { it.name to id }
            }.groupBy({ it.first }, { it.second })

            return CardSelectionValue(
                state = state,
                playerId = playerId,
                intents = intents,
                landsInPlay = landsInPlay.size,
                inPlayColors = inPlayColors,
                handLands = handLands,
                handSpells = handSpells,
                landDropNow = landDropNow,
                drawnLater = drawnLater,
                librarySize = library.size,
                libraryLands = libraryLands.size,
                libraryLandsByColor = byColor,
                legendsInPlay = legends,
                delayPerTurn = if (denial) DENIAL_DELAY_PER_TURN else DELAY_PER_TURN,
            )
        }
    }
}
