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
 */
@Component
class RandomDeckResolver(
    private val sealedDeckGenerator: SealedDeckGenerator,
    private val constructedDeckGenerator: ConstructedDeckGenerator,
    private val commanderDeckGenerator: CommanderDeckGenerator,
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
            val built = runCatching { constructedDeckGenerator.generate(setCodes, format) }
                .onFailure { error ->
                    logger.warn(
                        "Constructed deck for {} ({}) failed; falling back to a sealed pool",
                        format.displayName,
                        if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
                        error,
                    )
                }
                .getOrNull()
            if (built != null) return GeneratedDeck(built)
        }

        val sealed = if (setCodes.isEmpty()) {
            sealedDeckGenerator.generate(fallbackSetCode)
        } else {
            sealedDeckGenerator.generate(setCodes)
        }
        return GeneratedDeck(sealed)
    }
}
