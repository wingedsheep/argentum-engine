package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.gameserver.protocol.DeckEntryDTO
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.PrintingRef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Validates a deck list against the card registry and constructed-format rules.
 *
 * The format-agnostic baseline is: every card resolves, no banned/illegal cards, ≥ [DEFAULT_MIN_SIZE]
 * cards, ≤ 4 copies of any non-basic. Pass a [DeckFormat] to layer in format-specific rules:
 *
 *   - **Commander** (CR 903.5b): exact 100-card deck, singleton (max 1 of each non-basic). Cards
 *     whose oracle text contains "A deck can have any number of cards named X" override the
 *     singleton rule; cards saying "A deck can have up to N cards named X" cap at N (so e.g.
 *     Seven Dwarves caps at 7, Nazgûl at 9).
 *   - **Brawl**: 100-card singleton, same shape as Commander (Scryfall key `brawl`). The "any
 *     number of cards named X" oracle override applies here too, keeping Relentless Rats /
 *     Persistent Petitioners decks legal.
 *   - **Standard Brawl**: 60-card singleton restricted to the Standard pool (Scryfall key
 *     `standardbrawl`). Identical singleton + override rules as Brawl, just smaller.
 *   - **Standard / Pioneer / Modern / Pauper / Legacy / Vintage / Premodern**: 60-card minimum,
 *     4-of (current default). Format-specific banned-list enforcement comes from the per-card
 *     legality data Scryfall publishes — `card.legalFormats` already encodes "is currently legal
 *     in this format", so a banned card simply isn't in the set.
 *
 * Anything blocking (unknown card, count violation, illegal-in-format) becomes an error and
 * flips `valid = false`. Warnings are reserved for "unusual but allowed" (currently unused).
 */
@Component
class DeckValidator @Autowired constructor(
    private val cardRegistry: CardRegistry,
    /**
     * Optional — when injected, validation cross-checks every pinned [PrintingRef] in
     * `cardEntries` against this registry. When absent (e.g. unit tests that don't care
     * about printings), the printing check is skipped silently.
     */
    private val printingRegistry: PrintingRegistry? = null,
) {

    /** Convenience constructor for tests / callers that don't care about printings. */
    constructor(cardRegistry: CardRegistry) : this(cardRegistry, null)

    fun validate(
        deckList: Map<String, Int>,
        format: DeckFormat? = null,
        cardEntries: List<DeckEntryDTO>? = null,
        commanderPrinting: PrintingRef? = null,
    ): DeckValidationResult = runValidation(
        deckList = deckList,
        format = format,
        commander = null,
        commanderAware = false,
        cardEntries = cardEntries,
        commanderPrinting = commanderPrinting,
    )

    /**
     * Validate a [Deck] — the structured form that carries an explicit commander. For
     * commander-shaped formats this enables the full set of commander rules (eligibility,
     * color identity, library + command zone size). For other formats the [Deck.commander]
     * field is ignored.
     *
     * The library and the commander are merged into a single counts map for copy-cap and
     * format-legality checks (so e.g. submitting the commander a second time inside [Deck.cards]
     * trips the singleton cap).
     */
    fun validate(deck: Deck, format: DeckFormat? = null): DeckValidationResult {
        val commander = deck.commander
        val counts = deck.cards.groupingBy { it }.eachCount().toMutableMap()
        if (commander != null) {
            counts.merge(commander, 1, Int::plus)
        }
        // Mirror the rich-entry check from the Map overload: if the [Deck] carries pinned
        // printings, validate them against [printingRegistry].
        val richEntries = deck.cardEntries.takeIf { it.isNotEmpty() }?.map {
            DeckEntryDTO(it.name, it.printing)
        }
        return runValidation(
            counts,
            format,
            commander = commander,
            commanderAware = true,
            cardEntries = richEntries,
            commanderPrinting = deck.commanderPrinting,
        )
    }

    /**
     * @param commanderAware true when the caller has opted into commander rules (the [Deck]
     *   overload). Only commander-aware calls raise MISSING_COMMANDER for a null commander —
     *   the legacy [Map] overload doesn't surface a commander field, so silently skipping
     *   keeps existing submission paths working until they migrate to [Deck].
     */
    private fun runValidation(
        deckList: Map<String, Int>,
        format: DeckFormat?,
        commander: String?,
        commanderAware: Boolean,
        cardEntries: List<DeckEntryDTO>? = null,
        commanderPrinting: PrintingRef? = null,
    ): DeckValidationResult {
        val errors = mutableListOf<DeckValidationIssue>()
        val warnings = mutableListOf<DeckValidationIssue>()

        // Pinned-printing checks. Skipped when no [printingRegistry] is wired or when the
        // caller didn't supply rich entries. INVALID_PRINTING fires for: (a) the ref doesn't
        // resolve, (b) it resolves but its name doesn't match the entry's name (the client
        // tried to bind a Lightning Bolt entry to a Counterspell printing).
        val printings = printingRegistry
        if (printings != null) {
            cardEntries?.forEach { entry ->
                val ref = entry.printing ?: return@forEach
                val printing = printings.getPrinting(ref)
                if (printing == null) {
                    errors += DeckValidationIssue(
                        code = "INVALID_PRINTING",
                        message = "Unknown printing ${ref.identifier()} for ${entry.name}",
                        cardName = entry.name,
                    )
                } else if (printing.name != entry.name) {
                    errors += DeckValidationIssue(
                        code = "INVALID_PRINTING",
                        message = "Printing ${ref.identifier()} is ${printing.name}, not ${entry.name}",
                        cardName = entry.name,
                    )
                }
            }
            if (commander != null && commanderPrinting != null) {
                val printing = printings.getPrinting(commanderPrinting)
                when {
                    printing == null -> errors += DeckValidationIssue(
                        code = "INVALID_PRINTING",
                        message = "Unknown printing ${commanderPrinting.identifier()} for commander $commander",
                        cardName = commander,
                    )
                    printing.name != commander -> errors += DeckValidationIssue(
                        code = "INVALID_PRINTING",
                        message = "Printing ${commanderPrinting.identifier()} is ${printing.name}, not $commander",
                        cardName = commander,
                    )
                }
            }
        }

        // Filter out zero/negative counts up front; treat them as if they weren't submitted.
        val sanitized = deckList.filterValues { it > 0 }
        val totalCards = sanitized.values.sum()
        val profile = profileFor(format)

        // Group entries by their *base* card name so collector-number variants stack toward the copies cap.
        val countsByBaseName = mutableMapOf<String, Int>()
        for ((entry, count) in sanitized) {
            val card = cardRegistry.getCard(entry)
            if (card == null) {
                errors += DeckValidationIssue(
                    code = "UNKNOWN_CARD",
                    message = "Unknown card: \"$entry\"",
                    cardName = entry
                )
                continue
            }
            countsByBaseName.merge(card.name, count, Int::plus)
        }

        for ((cardName, count) in countsByBaseName) {
            val card = cardRegistry.getCard(cardName) ?: continue
            val limit = copyLimitFor(card, profile)
            if (limit != null && count > limit) {
                errors += DeckValidationIssue(
                    code = "TOO_MANY_COPIES",
                    message = copyLimitMessage(cardName, count, limit, profile),
                    cardName = cardName
                )
            }
            // Format legality. Cards with no recorded legality (custom test cards, missing Scryfall
            // data) are accepted silently — only an explicit "this card is not in $format" rejects.
            if (format != null && card.legalFormats.isNotEmpty() && format !in card.legalFormats) {
                errors += DeckValidationIssue(
                    code = "NOT_LEGAL_IN_FORMAT",
                    message = "$cardName is not legal in ${format.displayName}",
                    cardName = cardName
                )
            }
        }

        when {
            profile.exactSize != null && totalCards != profile.exactSize -> {
                errors += DeckValidationIssue(
                    code = if (totalCards < profile.exactSize) "TOO_FEW_CARDS" else "TOO_MANY_CARDS",
                    message = "${profile.formatLabel ?: "Deck"} requires exactly ${profile.exactSize} cards (have $totalCards)",
                    cardName = null
                )
            }
            totalCards < profile.minSize -> {
                errors += DeckValidationIssue(
                    code = "TOO_FEW_CARDS",
                    message = "Deck has $totalCards cards — minimum is ${profile.minSize}",
                    cardName = null
                )
            }
        }

        if (format != null && format.isCommanderShape && commanderAware) {
            validateCommanderRules(commander, countsByBaseName, format.displayName, errors)
        }

        return DeckValidationResult(
            valid = errors.isEmpty(),
            totalCards = totalCards,
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Enforces the commander-specific rules layered on top of the constructed baseline:
     * a designated commander is required, it must be a legal commander (CR 903.3), and every
     * other card's color identity must fit within the commander's (CR 903.4).
     *
     * Called only when [format] is commander-shaped. When [commander] is null, only the
     * MISSING_COMMANDER error is raised — color-identity checks need a known commander to
     * compare against. Callers using the legacy [Map] entry point pass null and skip identity
     * enforcement until the surrounding flow is updated to provide a [Deck].
     */
    private fun validateCommanderRules(
        commander: String?,
        countsByBaseName: Map<String, Int>,
        formatLabel: String,
        errors: MutableList<DeckValidationIssue>,
    ) {
        if (commander == null) {
            errors += DeckValidationIssue(
                code = "MISSING_COMMANDER",
                message = "$formatLabel decks require a designated commander",
                cardName = null,
            )
            return
        }
        val commanderCard = cardRegistry.getCard(commander)
        if (commanderCard == null) {
            // UNKNOWN_CARD already raised by the main loop if the commander is also missing
            // from the registry — no second error needed.
            return
        }
        if (!CommanderEligibility.isLegalCommander(commanderCard)) {
            errors += DeckValidationIssue(
                code = "INVALID_COMMANDER",
                message = "${commanderCard.name} cannot be a $formatLabel commander",
                cardName = commanderCard.name,
            )
            // Identity check against an illegal commander would be misleading — bail.
            return
        }

        val allowed: Set<Color> = commanderCard.colorIdentity
        for ((cardName, _) in countsByBaseName) {
            if (cardName == commanderCard.name) continue
            val card = cardRegistry.getCard(cardName) ?: continue
            val violating = card.colorIdentity - allowed
            if (violating.isNotEmpty()) {
                errors += DeckValidationIssue(
                    code = "COLOR_IDENTITY_VIOLATION",
                    message = colorIdentityMessage(cardName, violating, commanderCard.name, allowed),
                    cardName = cardName,
                )
            }
        }
    }

    private fun colorIdentityMessage(
        cardName: String,
        violating: Set<Color>,
        commanderName: String,
        allowed: Set<Color>,
    ): String {
        val violatingLabel = violating.joinToString(", ") { it.displayName }
        val allowedLabel = if (allowed.isEmpty()) "colorless" else allowed.joinToString(", ") { it.displayName }
        return "$cardName ($violatingLabel) is outside $commanderName's color identity ($allowedLabel)"
    }

    /**
     * Returns the maximum number of copies of [card] this format allows. `null` means
     * "unlimited" (basic lands, plus Commander cards saying "any number"). The default per-format
     * cap kicks in otherwise.
     */
    private fun copyLimitFor(card: CardDefinition, profile: FormatProfile): Int? {
        if (card.typeLine.isBasicLand) return null
        // Per-card override from oracle text — "A deck can have any number / up to N cards named X".
        // Applies in every format, but is most relevant under Commander's singleton rule.
        val override = parseDeckSizeOverride(card.oracleText, card.name)
        if (override != null) return override.cap
        return profile.maxCopiesNonBasic
    }

    private fun copyLimitMessage(cardName: String, count: Int, limit: Int, profile: FormatProfile): String {
        val basis = profile.formatLabel?.let { " ($it)" } ?: ""
        return when (limit) {
            1 -> "$count copies of $cardName — singleton allows only 1$basis"
            else -> "$count copies of $cardName — limit is $limit for non-basic cards$basis"
        }
    }

    private data class FormatProfile(
        val minSize: Int,
        val exactSize: Int? = null,
        val maxCopiesNonBasic: Int,
        val formatLabel: String? = null,
    )

    private fun profileFor(format: DeckFormat?): FormatProfile = when (format) {
        DeckFormat.COMMANDER, DeckFormat.BRAWL -> FormatProfile(
            // 100-card singleton. Commander eligibility and color identity are enforced by
            // validateCommanderRules() when the caller supplies a Deck (with explicit
            // commander). Partner / Background pairs are not yet supported — that's the
            // remaining structural rule a future revision needs to handle.
            minSize = SINGLETON_HUNDRED_DECK_SIZE,
            exactSize = SINGLETON_HUNDRED_DECK_SIZE,
            maxCopiesNonBasic = 1,
            formatLabel = format.displayName,
        )
        DeckFormat.STANDARD_BRAWL -> FormatProfile(
            // 60-card singleton, Standard pool. Per-card legality is sourced from Scryfall's
            // `standardbrawl` legality field; the construction shape is enforced here.
            minSize = STANDARD_BRAWL_DECK_SIZE,
            exactSize = STANDARD_BRAWL_DECK_SIZE,
            maxCopiesNonBasic = 1,
            formatLabel = format.displayName,
        )
        else -> FormatProfile(
            minSize = DEFAULT_MIN_SIZE,
            maxCopiesNonBasic = DEFAULT_MAX_COPIES_NON_BASIC,
            formatLabel = format?.displayName,
        )
    }

    /**
     * Validate a commander-shaped *drafted/sealed* deck against the player's [pool], not against
     * Scryfall's format legality. Used by [com.wingedsheep.gameserver.handler.LobbyHandler] for
     * the COMMANDER_DRAFT and COMMANDER_SEALED tournament formats, where:
     *
     *  - The pool is the legality universe — every non-basic deck card must appear in [pool] with
     *    at least the multiplicity the player included it. Basic lands are always allowed (the
     *    lobby supplies them separately).
     *  - The deck size has a minimum ([minDeckSize], typically 60) but no exact size — players
     *    can play any deck size ≥ the minimum.
     *  - Singleton is opt-in via [allowDuplicates]: when true (the lobby's default), no copy cap
     *    on non-basics. When false, paper-Commander singleton rules apply (max 1, with the usual
     *    "any number named X" oracle override).
     *  - The commander must be in [pool] and [CommanderEligibility.isLegalCommander]-legal, and
     *    every other card's colour identity must fit within the commander's identity.
     */
    fun validateCommanderLimited(
        deck: Deck,
        pool: List<CardDefinition>,
        minDeckSize: Int = 60,
        allowDuplicates: Boolean = true,
    ): DeckValidationResult {
        val errors = mutableListOf<DeckValidationIssue>()
        val warnings = mutableListOf<DeckValidationIssue>()

        val poolCounts: Map<String, Int> = pool.groupingBy { it.name }.eachCount()
        val commander = deck.commander

        val mainboardCounts: Map<String, Int> = deck.cards.groupingBy { it }.eachCount()
        val allCounts: Map<String, Int> = if (commander != null) {
            mainboardCounts.toMutableMap().apply { merge(commander, 1, Int::plus) }
        } else {
            mainboardCounts
        }

        // Resolve every submitted card; basic lands bypass the pool-membership check (the lobby
        // ships basics out-of-band via lobby.basicLands).
        for ((cardName, submitted) in allCounts) {
            val card = cardRegistry.getCard(cardName)
            if (card == null) {
                errors += DeckValidationIssue(
                    code = "UNKNOWN_CARD",
                    message = "Unknown card: \"$cardName\"",
                    cardName = cardName,
                )
                continue
            }
            if (card.typeLine.isBasicLand) continue

            val available = poolCounts[cardName] ?: 0
            if (submitted > available) {
                errors += DeckValidationIssue(
                    code = "NOT_IN_POOL",
                    message = "$cardName: $submitted submitted but only $available in your pool",
                    cardName = cardName,
                )
            }

            if (!allowDuplicates) {
                val limit = copyLimitFor(card, singletonProfile)
                if (limit != null && submitted > limit) {
                    errors += DeckValidationIssue(
                        code = "TOO_MANY_COPIES",
                        message = copyLimitMessage(cardName, submitted, limit, singletonProfile),
                        cardName = cardName,
                    )
                }
            }
        }

        val totalCards = allCounts.values.sum()
        if (totalCards < minDeckSize) {
            errors += DeckValidationIssue(
                code = "TOO_FEW_CARDS",
                message = "Deck has $totalCards cards — minimum is $minDeckSize",
                cardName = null,
            )
        }

        // Commander rules: required, legal commander, colour identity bounds the rest of the
        // deck. Reuse the shared helper so the error codes match the constructed path.
        validateCommanderRules(
            commander = commander,
            countsByBaseName = allCounts,
            formatLabel = "Commander Draft",
            errors = errors,
        )

        return DeckValidationResult(
            valid = errors.isEmpty(),
            totalCards = totalCards,
            errors = errors,
            warnings = warnings,
        )
    }

    /** Singleton-profile constant used by [validateCommanderLimited] when duplicates are disabled. */
    private val singletonProfile = FormatProfile(
        minSize = 0,
        maxCopiesNonBasic = 1,
        formatLabel = "Commander Draft (singleton)",
    )

    companion object {
        // Default constructed minimum for every non-Commander format. Pre-format submissions (no
        // format specified) also use this so the picker stays permissive when a player just
        // wants to mash 60 cards together for a casual game.
        const val DEFAULT_MIN_SIZE = 40
        const val DEFAULT_MAX_COPIES_NON_BASIC = 4
        // Shared by Commander (CR 903.5b) and Brawl (also a 100-card singleton format). Named
        // generically so a future 100-card singleton variant can reuse it without renaming.
        const val SINGLETON_HUNDRED_DECK_SIZE = 100
        const val STANDARD_BRAWL_DECK_SIZE = 60

        // "A deck can have any number of cards named <Name>" → unlimited.
        // "A deck can have up to <N> cards named <Name>"     → cap at N.
        // Both phrasings are matched case-insensitively against the card's own name to defend
        // against errata/typos in registered oracle text.
        private val ANY_NUMBER_RE = Regex(
            """A deck can have any number of cards named ([^.]+)\.""",
            RegexOption.IGNORE_CASE,
        )
        private val UP_TO_RE = Regex(
            """A deck can have up to (one|two|three|four|five|six|seven|eight|nine|ten|\d+) cards named ([^.]+)\.""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Detects a per-card deck-size override in [oracleText]. Returns null if no override is
         * declared, otherwise an [OverrideRule] with the cap (Int.MAX_VALUE = unlimited) and the
         * card name the rule references. Callers should still confirm the rule names *this* card
         * — it's not an override if the text mentions a different card.
         */
        fun parseDeckSizeOverride(oracleText: String, cardName: String): OverrideRule? {
            if (oracleText.isBlank()) return null
            ANY_NUMBER_RE.find(oracleText)?.let { match ->
                val named = match.groupValues[1].trim()
                if (named.equals(cardName, ignoreCase = true)) {
                    return OverrideRule(cap = Int.MAX_VALUE, named = named)
                }
            }
            UP_TO_RE.find(oracleText)?.let { match ->
                val word = match.groupValues[1].trim()
                val named = match.groupValues[2].trim()
                if (named.equals(cardName, ignoreCase = true)) {
                    val cap = wordToInt(word) ?: return null
                    return OverrideRule(cap = cap, named = named)
                }
            }
            return null
        }

        data class OverrideRule(val cap: Int, val named: String)

        private fun wordToInt(token: String): Int? = when (token.lowercase()) {
            "one" -> 1; "two" -> 2; "three" -> 3; "four" -> 4; "five" -> 5
            "six" -> 6; "seven" -> 7; "eight" -> 8; "nine" -> 9; "ten" -> 10
            else -> token.toIntOrNull()
        }
    }
}

data class DeckValidationResult(
    val valid: Boolean,
    val totalCards: Int,
    val errors: List<DeckValidationIssue>,
    val warnings: List<DeckValidationIssue>
)

data class DeckValidationIssue(
    val code: String,
    val message: String,
    val cardName: String?
)
