package com.wingedsheep.engine.registry

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Registry of per-set token art: which image a token shows when a card from a given set creates it.
 *
 * Sits alongside [CardRegistry] and [PrintingRegistry], and completes the same story for a kind of
 * object neither can describe. `CardRegistry` owns the canonical oracle; `PrintingRegistry` owns
 * per-print art for real *cards*. A token is neither — no `CardDefinition`, no `Printing` row — so
 * its art has nowhere to live except with the set that prints it. Sets contribute their rows via
 * `MtgSet.tokenArt`; the engine consults this at token creation.
 *
 * ## Why not put the art on the card
 * A card that bakes `imageUri` into its `CreateToken` effect mints the same art from every
 * printing. That is wrong as soon as the card is reprinted into a set that prints its own version
 * of the token — the reprint should mint the reprint's token. Keying on the *set the creating card
 * was printed in* is what makes reprints come out right, and it keeps art out of the card script.
 *
 * ## Resolution order (see the token executors)
 * 1. An explicit `imageUri` on the effect — a deliberate per-card override, always wins.
 * 2. This registry, for the set the creating card was printed in.
 * 3. The engine-wide generic fallback keyed by creature type
 *    ([com.wingedsheep.engine.handlers.effects.token.TokenArt]).
 *
 * A set may contribute several rows for one token — the same token printed with different
 * illustrations. [resolveAll] returns them all so a batch of tokens created at once shows the whole
 * run of arts; [resolve] takes the first for callers that only need one.
 *
 * ## Usage
 * ```kotlin
 * val tokenArt = TokenArtRegistry()
 * MtgSetCatalog.all.forEach { tokenArt.register(it.code, it.tokenArt, it.cards.map(CardDefinition::name)) }
 * tokenArt.resolve(sourceCardDefinitionId = "Arahbo, the First Fang#FDN-2", tokenName = "Cat")
 * ```
 */
class TokenArtRegistry {

    private val bySet = mutableMapOf<String, MutableList<TokenPrinting>>()

    /**
     * Set code owning each card name, for the sets that declare token art. Only those sets are
     * indexed — this is a fallback for resolving the owning set when the creating entity's
     * definition id carries no printing (scenario builders mint entities keyed by bare card name),
     * and there is no reason to carry every name in the corpus for it.
     */
    private val setByCardName = mutableMapOf<String, String>()

    /**
     * Register one set's token art. [cardNames] are the names of the cards in that set, used to
     * resolve the owning set when a creating entity is keyed by bare name rather than by printing.
     * A set that prints no tokens of its own is a no-op.
     *
     * Re-registering a set code appends; register each set once.
     */
    fun register(setCode: String, tokenArt: List<TokenPrinting>, cardNames: Iterable<String> = emptyList()) {
        if (tokenArt.isEmpty()) return
        bySet.getOrPut(setCode) { mutableListOf() }.addAll(tokenArt)
        // First writer wins: a name printed in several art-declaring sets keeps its earliest
        // registration, and the printing-qualified path below is what disambiguates in practice.
        for (name in cardNames) setByCardName.putIfAbsent(name, setCode)
    }

    /**
     * Art for a token being created, or null when the creating card's set prints none that match.
     *
     * @param sourceCardDefinitionId The creating entity's `CardComponent.cardDefinitionId`, the
     *   fallback when no printing is known. Real games key entities as `"Name#SET-CN"` naming the
     *   *canonical* definition (that's deliberate — ability lookups resolve through it), so this
     *   yields the set the card was first printed in. Scenario builders key by bare name, which
     *   falls back to [setByCardName].
     * @param sourcePrintingSetCode The creating entity's `CardComponent.printingSetCode` — the
     *   printing the player actually brought. Wins when known, which is what makes a reprint mint
     *   its own set's token rather than the original's.
     * @param tokenName Token name as it will be created ("Cat", "Zombie Druid").
     */
    fun resolve(
        sourceCardDefinitionId: String?,
        tokenName: String,
        power: Int? = null,
        toughness: Int? = null,
        colors: Set<Color> = emptySet(),
        sourcePrintingSetCode: String? = null,
    ): String? = resolveAll(
        sourceCardDefinitionId, tokenName, power, toughness, colors, sourcePrintingSetCode,
    ).firstOrNull()

    /**
     * Every art the creating card's set printed for this token, in declaration order; empty when
     * the set prints none that match.
     *
     * A set that printed one token with several illustrations declares one row per art (Jumpstart's
     * four Dogs). The token executors deal this list out across a batch created at once, so four
     * Dogs made by one spell show the four printed arts rather than one repeated — hence plural
     * here and [resolve] as the singular front door onto the same lookup.
     */
    fun resolveAll(
        sourceCardDefinitionId: String?,
        tokenName: String,
        power: Int? = null,
        toughness: Int? = null,
        colors: Set<Color> = emptySet(),
        sourcePrintingSetCode: String? = null,
    ): List<String> {
        val setCode = setCodeFor(sourceCardDefinitionId, sourcePrintingSetCode) ?: return emptyList()
        val rows = bySet[setCode] ?: return emptyList()
        return TokenPrinting.allMatches(rows, tokenName, power, toughness, colors)
            .map { it.imageUri }
    }

    /** Set code that should supply token art for a creating entity, or null if unknown. */
    private fun setCodeFor(cardDefinitionId: String?, printingSetCode: String?): String? {
        // The printing the player actually brought is the answer whenever it's known, and it's the
        // only one that can be right for a reprint: a definition id keeps the *oracle* definition's
        // coordinates, so an Innistrad Remastered Garruk still reads `#ISD-181` below.
        if (printingSetCode != null && printingSetCode in bySet) return printingSetCode
        val id = cardDefinitionId ?: return null
        // "Name#SET-CN" — the printing the entity was built from. A bare "Name#CN" (definitions
        // with a collector number but no set) has no dash and correctly falls through to the name
        // index rather than mistaking the collector number for a set code.
        val printingSuffix = id.substringAfter('#', "")
        if (printingSuffix.contains('-')) {
            val setCode = printingSuffix.substringBefore('-')
            if (setCode in bySet) return setCode
        }
        return setByCardName[id.substringBefore('#')]
    }

    /** Number of sets contributing token art. */
    val size: Int get() = bySet.size

    fun clear() {
        bySet.clear()
        setByCardName.clear()
    }
}
