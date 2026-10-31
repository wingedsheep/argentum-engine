package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.gameserver.controller.DecksController
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.gameserver.protocol.DeckRequestConverter
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * Picks a *different* built-in starter deck from the same set(s) as the human's deck, for an AI
 * seat on Auto.
 *
 * The starters come in families built against each other — Bloomburrow's six two-colour tribal
 * decks, the Standard lists — so a sibling is the fairest and most characterful opponent there is:
 * a tuned 60-card list facing a tuned 60-card list, both playing the set's mechanics. A deck
 * generated from the same pool is the fallback, not the first choice, because an auto-build from a
 * whole set is rarely as focused as a list someone curated.
 *
 * A candidate qualifies when it is built from the human's sets ([MIN_COVERAGE] of its non-basics
 * printed there), isn't the human's own deck in disguise ([MAX_OVERLAP]), has the shape the lobby
 * plays (a commander exactly when the game uses commanders), and passes the lobby's format.
 *
 * A constructed deck spread over many sets has no set identity to match, so the resolver matches
 * it by format instead ([matchFormat]).
 */
@Component
class PremadeOpponentPicker(
    private val setInference: DeckSetInference,
    private val deckValidator: DeckValidator,
) {
    /**
     * A sibling starter for [opponentDeck], or null when none qualifies.
     *
     * @param opponentDeck the human's submitted list (commander included, as the lobby holds it).
     * @param opponentSets the sets [DeckSetInference.setsOf] read off it; empty never matches.
     */
    fun pick(
        opponentDeck: Map<String, Int>,
        opponentSets: List<String>,
        format: DeckFormat?,
        commanderRules: Boolean,
        random: Random = Random.Default,
    ): GeneratedDeck? {
        if (opponentSets.isEmpty()) return null
        val opponentNames = setInference.nonBasicNames(opponentDeck)
        return DecksController.EXAMPLE_DECKS
            .filter { (it.commander != null) == commanderRules }
            .filter { setInference.coverage(it.cards, opponentSets) >= MIN_COVERAGE }
            .filter { overlap(setInference.nonBasicNames(it.cards), opponentNames) <= MAX_OVERLAP }
            .filter { format == null || isLegal(it, format) }
            .randomOrNull(random)
            ?.let { starter ->
                val commanders = listOfNotNull(starter.commander)
                GeneratedDeck(DeckRequestConverter.stripCommanders(starter.cards, commanders), starter.commander)
            }
    }

    /**
     * A built-in 60-card starter legal in [format] for an AI seat facing [opponentDeck] — a
     * constructed list matched by format rather than by set — or null when none qualifies.
     *
     * The curated lists beat any generated deck: they are tuned four-of lists with a plan, where a
     * build from the whole format is at best the format's highest-rated cards in two colours. The
     * same [MAX_OVERLAP] rule keeps the AI off the human's own starter. Only constructed-sized,
     * commander-less starters count, and only when the validator passes them in [format] — so a
     * rotated card or a Pauper lobby simply leaves nothing to pick and the caller generates instead.
     */
    fun pickForFormat(opponentDeck: Map<String, Int>, format: DeckFormat, random: Random = Random.Default): GeneratedDeck? {
        if (format.isCommanderShape) return null
        val opponentNames = setInference.nonBasicNames(opponentDeck)
        return DecksController.EXAMPLE_DECKS
            .filter { it.commander == null && it.cards.values.sum() >= CONSTRUCTED_DECK_SIZE }
            .filter { overlap(setInference.nonBasicNames(it.cards), opponentNames) <= MAX_OVERLAP }
            .filter { isLegal(it, format) }
            .randomOrNull(random)
            ?.let { GeneratedDeck(it.cards) }
    }

    /**
     * The format to build an opponent for [deck] in, or null when none applies.
     *
     * In order: the format the deck [declared] it was built for (checked legal when it was
     * submitted), when the lobby admits it; else the lobby's own [lobbyFormat] — narrowed, on the
     * Standard-to-Vintage ladder, to the smallest step the deck is legal in, so a Standard deck in
     * a Modern lobby meets a Standard deck; else, with no lobby restriction, the narrowest step of
     * that ladder the deck is legal in. Commander-shaped formats never apply — a commander game
     * builds its own deck.
     */
    fun matchFormat(deck: Map<String, Int>, declared: DeckFormat?, lobbyFormat: DeckFormat?): DeckFormat? {
        if (declared != null && !declared.isCommanderShape && admits(lobbyFormat, declared)) return declared
        return when {
            lobbyFormat == null -> narrowestLegal(deck, NESTED_FORMATS)
            lobbyFormat in NESTED_FORMATS ->
                narrowestLegal(deck, NESTED_FORMATS.subList(0, NESTED_FORMATS.indexOf(lobbyFormat) + 1))
            lobbyFormat.isCommanderShape -> null
            else -> lobbyFormat
        }
    }

    private fun narrowestLegal(deck: Map<String, Int>, formats: List<DeckFormat>): DeckFormat? =
        formats.firstOrNull { deckValidator.validate(deck, it).valid }

    /** Whether a lobby restricted to [lobbyFormat] accepts a deck built for [format]. */
    private fun admits(lobbyFormat: DeckFormat?, format: DeckFormat): Boolean = when {
        lobbyFormat == null || lobbyFormat == format -> true
        lobbyFormat in NESTED_FORMATS && format in NESTED_FORMATS ->
            NESTED_FORMATS.indexOf(format) <= NESTED_FORMATS.indexOf(lobbyFormat)
        else -> false
    }

    private fun isLegal(starter: DecksController.ExampleDeckDTO, format: DeckFormat): Boolean =
        if (starter.commander != null) {
            val library = DeckRequestConverter.stripCommanders(starter.cards, listOf(starter.commander))
            deckValidator.validate(
                Deck(cards = library.flatMap { (name, n) -> List(n) { name } }, commander = starter.commander),
                format,
            ).valid
        } else {
            deckValidator.validate(starter.cards, format).valid
        }

    /** Jaccard similarity of two name sets. */
    private fun overlap(a: Set<String>, b: Set<String>): Double {
        val union = (a + b).size
        return if (union == 0) 1.0 else (a intersect b).size.toDouble() / union
    }

    private companion object {
        const val MIN_COVERAGE = 0.9
        const val CONSTRUCTED_DECK_SIZE = 60
        /** Past this, the "sibling" is the human's own starter (or a light edit of it). */
        const val MAX_OVERLAP = 0.5

        /** The formats whose card pools nest, smallest first; see [matchFormat]. */
        val NESTED_FORMATS = listOf(
            DeckFormat.STANDARD,
            DeckFormat.PIONEER,
            DeckFormat.MODERN,
            DeckFormat.LEGACY,
            DeckFormat.VINTAGE,
        )
    }
}
