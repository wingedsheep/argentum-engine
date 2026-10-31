package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.engine.deck.CommanderDeckGenerator
import com.wingedsheep.ai.engine.deck.ConstructedDeckGenerator
import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.gameserver.lobby.AiDeckSpec
import com.wingedsheep.sdk.core.DeckFormat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Builds the decklist for any quick-lobby seat that didn't bring one — the AI seat's [AiDeckSpec],
 * and a human seat that picked "Random".
 *
 * Both seats go through here so the two axes — *what the seat asked for* and *what the lobby's
 * format allows* — are crossed in exactly one place. They used to be crossed in two: the AI seat
 * honoured the lobby format while a human on Random always got a 40-card sealed pool, so a Pauper
 * lobby could seat a rare-filled sealed deck opposite a legal 60-card Pauper deck. The matrix:
 *
 * |                | no format / limited lobby   | constructed format | commander-shape format |
 * |----------------|-----------------------------|--------------------|------------------------|
 * | AI [AiDeckSpec.Auto]  | sealed pool, human's set | 60-card legal deck, whole card base | singleton deck + commander, whole card base |
 * | AI [AiDeckSpec.Sets]  | sealed pool, chosen sets | 60-card legal deck, chosen sets | singleton deck + commander, chosen sets |
 * | AI [AiDeckSpec.Fixed] | the submitted list       | the submitted list (validated on submit) | the submitted list and its commander |
 * | human "Random", no sets | sealed pool, rolled set | 60-card legal deck, whole card base | singleton deck + commander, whole card base |
 * | human "Random", sets    | sealed pool, chosen sets | 60-card legal deck, chosen sets | singleton deck + commander, chosen sets |
 *
 * A seat's *set* choice narrows the pool on every path, not just the limited one: it picks which
 * boosters to open under no format, and which sets the constructed / commander builders may draw
 * from under one. So the human's two rows are the AI's [AiDeckSpec.Auto] and [AiDeckSpec.Sets], and
 * a human on Random has the same reach over their own seat as the host has over the AI's.
 *
 * Every non-commander path also hands back a sideboard — the cards the seat owns outside the game
 * (CR 400.11a): the unplayed rest of a sealed pool (CR 100.4b), or a fifteen-card wishboard from a
 * constructed deck's legal pool. A seat that never built a deck still has one for Learn and the
 * wishes to reach into.
 *
 * Every path returns a [GeneratedDeck] rather than a bare decklist, because a commander-shape lobby
 * has *two* answers to "what does this seat play" and they have to be decided together. Before
 * [CommanderDeckGenerator] existed there was only one, and commander shapes fell through to a
 * limited deck with no commander that the engine then refused to start.
 *
 * **Commander-ness is its own parameter, not a reading of [DeckFormat].** A lobby says "this game
 * has commanders" on its Rules axis; a commander-shaped deck *legality* only defaults that axis.
 * A premade Commander pod with no legality restriction is the case that separates them, and
 * inferring from the format alone would hand its AI seats a commander-less deck the engine refuses
 * to start. Callers pass what their lobby's Rules axis says.
 *
 * **An Auto AI seat facing a submitted deck plays out of that deck's sets** ([resolveAgainst]). A
 * human who brings a Bloomburrow starter shouldn't meet a sealed pool rolled from an unrelated set;
 * they get, in order of preference: a *different* built-in starter from the same set(s)
 * ([PremadeOpponentPicker] — the starter families are tuned against each other), else a deck built
 * from those sets in the shape of the human's own (a 60-card constructed build against a 60-card
 * list, a sealed pool against a 40-card one), else the unmatched answer above.
 *
 * **A constructed list with no set identity is matched by format instead.** A Standard deck
 * spread over a dozen sets has nothing to match, and its unmatched answer was a sealed pool from a
 * random set — 40 singletons against a tuned 60, which the AI lost nine times in ten. It now meets
 * a deck for the format the list was built for: the one its saved deck or example declares, else
 * the lobby's, else the narrowest of the nested constructed formats it is legal in
 * ([PremadeOpponentPicker.matchFormat]) — a built-in starter legal there when one qualifies, else a
 * deck generated from the format's card base. A human on Random keeps the unmatched answer.
 */
@Component
class RandomDeckResolver(
    private val sealedDeckGenerator: SealedDeckGenerator,
    private val constructedDeckGenerator: ConstructedDeckGenerator,
    private val commanderDeckGenerator: CommanderDeckGenerator,
    private val setInference: DeckSetInference,
    private val premadePicker: PremadeOpponentPicker,
) {
    private val logger = LoggerFactory.getLogger(RandomDeckResolver::class.java)

    /**
     * The AI seat's deck.
     *
     * @param spec what the host chose for the AI seat.
     * @param format the lobby's deck-format restriction, or null for none.
     * @param fallbackSetCode the set the lobby already resolved for its random pools — used by
     *        [AiDeckSpec.Auto] on the limited path so the AI and the human open the same set.
     * @param commanderRules whether this lobby's games use commanders.
     */
    fun resolve(
        spec: AiDeckSpec,
        format: DeckFormat?,
        fallbackSetCode: String,
        commanderRules: Boolean,
    ): GeneratedDeck {
        // A fixed list is the host's explicit answer and was validated against the format when it
        // was submitted; nothing left to decide. Its commander rides along — under a non-commander
        // format the caller drops it, exactly as it does for a human's saved deck.
        if (spec is AiDeckSpec.Fixed) {
            return GeneratedDeck(spec.deckList, spec.commander?.takeIf { it.isNotBlank() })
        }

        // An empty set selection means the host cleared the picker rather than that they want an
        // empty pool — treat it as Auto instead of failing the game start.
        val setCodes = (spec as? AiDeckSpec.Sets)?.setCodes?.filter { it.isNotBlank() }.orEmpty()

        return randomDeck(format, setCodes, fallbackSetCode, commanderRules)
    }

    /**
     * The AI seat's deck in a quick lobby, matched to [opponentDeck] — the human's submitted list —
     * when the host left the AI on [AiDeckSpec.Auto]. See the class doc for the order of answers.
     * Any other spec, a human on Random (null or empty [opponentDeck]), or a deck with no clear set
     * identity resolves exactly as [resolve] does.
     */
    fun resolveAgainst(
        spec: AiDeckSpec,
        format: DeckFormat?,
        fallbackSetCode: String,
        commanderRules: Boolean,
        opponentDeck: Map<String, Int>?,
        opponentDeckFormat: DeckFormat? = null,
    ): GeneratedDeck {
        val sets = if (spec is AiDeckSpec.Auto && !opponentDeck.isNullOrEmpty()) {
            setInference.setsOf(opponentDeck)
        } else {
            emptyList()
        }
        if (opponentDeck == null || sets.isEmpty()) {
            if (spec is AiDeckSpec.Auto && opponentDeck != null) {
                formatMatchedDeck(opponentDeck, opponentDeckFormat, format, commanderRules)?.let { return it }
            }
            return resolve(spec, format, fallbackSetCode, commanderRules)
        }

        premadePicker.pick(opponentDeck, sets, format, commanderRules)?.let { starter ->
            logger.info("AI plays a sibling starter from {} against the human's deck", sets.joinToString(", "))
            return starter
        }
        // Built out of the deck's sets, to the format it was built for when it declared one.
        val buildFormat = if (opponentDeckFormat != null && !commanderRules) {
            premadePicker.matchFormat(opponentDeck, opponentDeckFormat, format) ?: format
        } else {
            format
        }
        return matchedDeck(buildFormat, sets, commanderRules, opponentDeck.values.sum())
            // The deck's sets couldn't support a build of their own (too few legal spells or
            // creatures): a constructed list still deserves a constructed opponent, so build for
            // its format from the whole card base before dropping to the unmatched sealed answer.
            ?: formatWideFallback(opponentDeck, opponentDeckFormat, format, commanderRules)
            ?: resolve(spec, format, fallbackSetCode, commanderRules)
    }

    /**
     * The format-wide build behind a failed set-matched one: the same deck [formatMatchedDeck]
     * builds for a list with no set identity. Null outside a commander game's constructed-sized
     * list, or when no format applies.
     */
    private fun formatWideFallback(
        opponentDeck: Map<String, Int>,
        opponentDeckFormat: DeckFormat?,
        format: DeckFormat?,
        commanderRules: Boolean,
    ): GeneratedDeck? {
        if (commanderRules || opponentDeck.values.sum() < CONSTRUCTED_DECK_SIZE) return null
        val target = premadePicker.matchFormat(opponentDeck, opponentDeckFormat, format) ?: return null
        logger.info("AI's set-matched deck failed; building a {} deck from the whole format instead", target.displayName)
        return starterOrBuildFor(opponentDeck, target)
    }

    /**
     * A deck for the format [opponentDeck] — a constructed list with no set identity — is played in
     * ([matchedFormat]): a built-in starter legal in that format when one qualifies
     * ([PremadeOpponentPicker.pickForFormat]), else one generated from the format's whole card base.
     * The starters come first because a curated list is a tuned deck with a plan, which no build
     * from ratings matches (2026-10-31: generated Standard decks went 0/4 against tuned lists); the
     * pick among them is random, so the AI's list still turns over from game to game. Null — the
     * caller's unmatched answer — when no format applies.
     */
    private fun formatMatchedDeck(
        opponentDeck: Map<String, Int>,
        opponentDeckFormat: DeckFormat?,
        format: DeckFormat?,
        commanderRules: Boolean,
    ): GeneratedDeck? {
        val target = matchedFormat(opponentDeck, opponentDeckFormat, format, commanderRules) ?: return null
        logger.info("AI builds a {} deck to match the human's", target.displayName)
        return starterOrBuildFor(opponentDeck, target)
    }

    private fun starterOrBuildFor(opponentDeck: Map<String, Int>, target: DeckFormat): GeneratedDeck? {
        premadePicker.pickForFormat(opponentDeck, target)?.let { starter ->
            logger.info("AI plays a {}-legal starter against the human's deck", target.displayName)
            return starter
        }
        return buildForFormat(target)
    }

    private fun buildForFormat(target: DeckFormat): GeneratedDeck? =
        runCatching { constructedDeckGenerator.generateDeck(emptyList(), target) }
            .onFailure { logger.warn("Format-matched {} deck failed", target.displayName, it) }
            .getOrNull()

    /**
     * The format an Auto AI seat builds for against [opponentDeck], or null when it plays the
     * deck's sets or the unmatched answer instead: a constructed-sized list with no set identity,
     * outside a commander game. See [PremadeOpponentPicker.matchFormat] for which format.
     */
    fun matchedFormat(
        opponentDeck: Map<String, Int>?,
        opponentDeckFormat: DeckFormat?,
        format: DeckFormat?,
        commanderRules: Boolean,
    ): DeckFormat? {
        if (opponentDeck.isNullOrEmpty() || commanderRules || opponentDeck.values.sum() < CONSTRUCTED_DECK_SIZE) return null
        if (setInference.setsOf(opponentDeck).isNotEmpty()) return null
        return premadePicker.matchFormat(opponentDeck, opponentDeckFormat, format)
    }

    /** The sets an Auto AI seat would match [opponentDeck] to; empty when it wouldn't match. */
    fun matchingSets(opponentDeck: Map<String, Int>?): List<String> =
        if (opponentDeck.isNullOrEmpty()) emptyList() else setInference.setsOf(opponentDeck)

    /**
     * A deck generated from [setCodes] in the shape the human brought, or null when those sets
     * can't fill one — the caller then drops the match rather than seat a broken deck.
     */
    private fun matchedDeck(
        format: DeckFormat?,
        setCodes: List<String>,
        commanderRules: Boolean,
        opponentDeckSize: Int,
    ): GeneratedDeck? {
        val commanderFormat = when {
            format != null && format.isCommanderShape -> format
            commanderRules -> DeckFormat.COMMANDER
            else -> null
        }
        val where = setCodes.joinToString(", ")
        return when {
            commanderFormat != null ->
                runCatching { commanderDeckGenerator.generate(setCodes, commanderFormat) }
                    .onFailure { logger.warn("Matched commander deck from {} failed", where, it) }
                    .getOrNull()
            // A constructed-sized list deserves a constructed opponent. With no lobby restriction,
            // Legacy is the widest pool that still keeps a generated deck off four-of power cards.
            format != null || opponentDeckSize >= CONSTRUCTED_DECK_SIZE ->
                runCatching { constructedDeckGenerator.generateDeck(setCodes, format ?: DeckFormat.LEGACY) }
                    .onFailure { logger.warn("Matched constructed deck from {} failed", where, it) }
                    .getOrNull()
            else ->
                runCatching { sealedDeckGenerator.generateDeck(setCodes) }
                    .onFailure { logger.warn("Matched sealed deck from {} failed", where, it) }
                    .getOrNull()
        }
    }

    /**
     * An AI seat's deck in a tournament lobby, where the lobby's own set selection stands in for the
     * quick lobby's "the same set as the human" and an empty selection means any set rather than a
     * failure. Same three answers, same order of preference — the two lobby kinds differ only in
     * where the fallback set comes from.
     */
    fun resolve(
        spec: AiDeckSpec,
        format: DeckFormat?,
        setCodes: List<String>,
        commanderRules: Boolean,
    ): GeneratedDeck = resolve(spec, format, fallbackSetFrom(setCodes), commanderRules)

    /**
     * A generated deck for a seat that pinned no set of its own — [resolve] without a spec, for the
     * seats that never had one to state.
     */
    fun randomDeck(format: DeckFormat?, setCodes: List<String>, commanderRules: Boolean): GeneratedDeck {
        val pinned = setCodes.filter { it.isNotBlank() }
        return randomDeck(format, pinned, fallbackSetFrom(pinned), commanderRules)
    }

    /** The set to open boosters from when nothing pinned one: the lobby's first, else any set. */
    private fun fallbackSetFrom(setCodes: List<String>): String =
        setCodes.firstOrNull { it.isNotBlank() } ?: sealedDeckGenerator.randomSetCode()

    /**
     * A generated deck for a seat with no submitted list, honouring the lobby's [format].
     *
     * Each branch falls back to the one below it rather than failing the game start: a commander
     * build that can't find a legal commander in a narrow set selection drops to the constructed
     * path, and a constructed build with too thin a legal pool drops to a sealed one. A deck the
     * seat can actually play beats an error message — with one exception, noted at the commander
     * branch, where the fallback is itself unplayable.
     *
     * @param format the lobby's deck-format restriction, or null for none.
     * @param setCodes sets the seat pinned its pool to; empty means "whatever the lobby resolved",
     *        i.e. [fallbackSetCode] on the limited path and the whole legal card base on the
     *        constructed one.
     * @param fallbackSetCode the single set to open boosters from when [setCodes] is empty.
     * @param commanderRules whether this lobby's games use commanders — see the class doc for why
     *        this is asked separately from [format].
     */
    fun randomDeck(
        format: DeckFormat?,
        setCodes: List<String>,
        fallbackSetCode: String,
        commanderRules: Boolean,
    ): GeneratedDeck {
        // Which commander-shaped format to build to: the lobby's own when it set one, else paper
        // Commander — the broadest commander-legal pool, and the right default for a lobby that
        // asked for commanders without restricting legality.
        val commanderFormat = when {
            format != null && format.isCommanderShape -> format
            commanderRules -> DeckFormat.COMMANDER
            else -> null
        }
        if (commanderFormat != null) {
            // Commander lobby: pick a commander and build a singleton deck inside its colour
            // identity. Falling through to a commander-less deck is *not* a graceful degradation
            // here — the engine refuses to start a commander game without one — so the caller has
            // to notice a null commander and refuse the start rather than seat a broken deck.
            val built = runCatching { commanderDeckGenerator.generate(setCodes, commanderFormat) }
                .onFailure { error ->
                    logger.warn(
                        "Commander deck for {} ({}) failed",
                        commanderFormat.displayName,
                        if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
                        error,
                    )
                }
                .getOrNull()
            if (built != null) return built
            logger.warn(
                "No {} deck could be built from {}; falling back to a deck with no commander",
                commanderFormat.displayName,
                if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
            )
        } else if (format != null) {
            // Constructed lobby: build to the format so both sides of the table play under the
            // same restriction. Falls back to the limited path if the legal pool is unusably thin
            // (a narrow format crossed with a narrow set selection).
            val built = runCatching { constructedDeckGenerator.generateDeck(setCodes, format) }
                .onFailure { error ->
                    logger.warn(
                        "Constructed deck for {} ({}) failed; falling back to a sealed pool",
                        format.displayName,
                        if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
                        error,
                    )
                }
                .getOrNull()
            if (built != null) return built
        }

        return if (setCodes.isEmpty()) {
            sealedDeckGenerator.generateDeck(fallbackSetCode)
        } else {
            sealedDeckGenerator.generateDeck(setCodes)
        }
    }

    private companion object {
        /** A submitted list at least this big is a constructed deck, not a limited one. */
        const val CONSTRUCTED_DECK_SIZE = 60
    }
}
