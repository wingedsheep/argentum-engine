package com.wingedsheep.gameserver.config

import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.engine.deck.RandomDeckGenerator
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.definitions.custom.JustOneGlassToken
import com.wingedsheep.mtg.sets.definitions.custom.SekshaasEarlySleeper
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.legality.LegalityData
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameBeansConfig(
    private val gameProperties: GameProperties,
) {

    private fun activeSets(): List<MtgSet> =
        MtgSetCatalog.all.filter { gameProperties.sets.isEnabled(it.code) }

    @Bean
    fun activeMtgSets(): List<MtgSet> = activeSets()

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        register(PredefinedTokens.allTokens)
        // Register every catalogued set so the standalone deckbuilder can browse and validate
        // cards from all sets, even those gated out of sealed/draft via game.sets.disabled-by-default.
        // Booster/sealed/draft generation still respects the active-set filter via boosterGenerator.
        for (set in MtgSetCatalog.all) {
            register(set.cards.stamp(set).withLegalities())
            register(set.basicLands.withLegalities())
            set.basicLandsFallback?.let { register(it.basicLands.withLegalities()) }
        }
        // Easter egg card — injected into Rick's deck at game start
        register(LegalityData.stamp(SekshaasEarlySleeper))
        register(LegalityData.stamp(JustOneGlassToken))
    }

    /**
     * Per-printing index. Populated in two passes:
     *
     * 1. Synthesised defaults from every registered card — one printing row per
     *    `CardDefinition` derived from its `setCode` + `metadata.collectorNumber`.
     *    This covers the canonical printing of every card.
     * 2. Explicit reprint rows contributed by each [MtgSet] via `MtgSet.printings`.
     *    Reprints overwrite synthesised entries with the same `(setCode, collectorNumber)`
     *    so a hand-curated reprint always wins over auto-derivation.
     *
     * Real Scryfall printing data lands in a later phase via a classpath-loaded jsonl;
     * until then these two passes are enough for the deckbuilder picker and the
     * game-init art override to function.
     */
    @Bean
    fun printingRegistry(cardRegistry: CardRegistry): PrintingRegistry = PrintingRegistry().apply {
        for (name in cardRegistry.allCardNames()) {
            cardRegistry.getCardsByName(name).forEach(::registerSynthesizedDefault)
        }
        for (set in MtgSetCatalog.all) {
            register(set.printings)
        }
    }

    @Bean
    fun boosterGenerator(cardRegistry: CardRegistry): BoosterGenerator = BoosterGenerator(
        // Every set with a non-empty card pool is selectable, not just the sealed-curated few.
        // Sets that aren't `sealedSupported` (or are flagged incomplete) ride along as "partial":
        // clients hide them behind a default-off toggle, but a host can still pick them. An empty
        // pool can't produce a booster, so those are the only sets excluded here — and an
        // all-reprint set's pool is resolved from its printings (see [boosterCardPool]) so it
        // isn't wrongly treated as empty.
        activeSets()
            .mapNotNull { set ->
                val pool = set.boosterCardPool(cardRegistry)
                if (pool.isEmpty()) null else set.code to set.toBoosterSetConfig(pool)
            }
            .toMap()
    )

    @Bean
    fun sealedDeckGenerator(boosterGenerator: BoosterGenerator): SealedDeckGenerator =
        SealedDeckGenerator(boosterGenerator)

    @Bean
    fun randomDeckGenerator(): RandomDeckGenerator {
        val active = activeSets()
        return RandomDeckGenerator(
            cardPool = active.flatMap { it.cards },
            basicLandVariants = PortalSet.basicLands,
            setCodes = active.map { it.code },
        )
    }
}

/**
 * Stamp each card with its [set]'s identity that the bare `CardDefinition` doesn't carry:
 * `setCode` (so the deckbuilder can resolve a default printing) and `metadata.releaseDate`
 * (so the synthesised default printing dates from the set, not `null`).
 *
 * The release-date stamp is what keeps the deckbuilder defaulting to the *plain* frame: a set's
 * showcase/borderless variant printings ship explicit release dates, but the canonical printing
 * is synthesised from this metadata. Without the stamp it dates to `null` and sorts *after* the
 * dated variants, so the catalog defaults to a showcase/borderless art. Stamping the set date
 * ties the canonical printing with its variants, letting the `isAlternateFrame` tiebreaker in
 * `PrintingRegistry.defaultPrinting` pick the plain one. Only fills gaps — never overwrites a
 * value a card already declares.
 */
private fun List<CardDefinition>.stamp(set: MtgSet): List<CardDefinition> =
    map { card ->
        val withSet = if (card.setCode == null) card.copy(setCode = set.code) else card
        if (withSet.metadata.releaseDate == null && set.releaseDate != null) {
            withSet.copy(metadata = withSet.metadata.copy(releaseDate = set.releaseDate))
        } else {
            withSet
        }
    }

private fun List<CardDefinition>.withLegalities(): List<CardDefinition> =
    map { LegalityData.stamp(it) }

/**
 * The card pool a set contributes to booster / sealed / draft generation.
 *
 * Sets that author their own [MtgSet.cards] use those as-is. An all-reprint set (e.g. Eighth
 * Edition) declares no own definitions — every card is a [Printing] whose canonical
 * [CardDefinition] lives in an earlier set — so each reprint is resolved to its canonical via
 * [registry] and overlaid with the reprint's presentation (set code, art) and its per-set
 * [com.wingedsheep.sdk.model.Printing.rarity] (a card's rarity differs between sets, and booster
 * generation slots by rarity). Reprints whose canonical isn't implemented anywhere are skipped.
 *
 * Without this, an all-reprint set has an empty `cards` list, is filtered out of
 * [boosterGenerator], and never appears in the selectable-set list (the Eighth Edition bug).
 */
private fun MtgSet.boosterCardPool(registry: CardRegistry): List<CardDefinition> {
    if (cards.isNotEmpty()) return cards
    return printings.mapNotNull { printing ->
        registry.getCardsByName(printing.name).firstOrNull()?.let { canonical ->
            val withArt = canonical.withPrinting(printing)
            withArt.copy(metadata = withArt.metadata.copy(rarity = printing.rarity))
        }
    }
}

private fun MtgSet.toBoosterSetConfig(cards: List<CardDefinition>): BoosterGenerator.SetConfig =
    BoosterGenerator.SetConfig(
        setCode = code,
        setName = displayName,
        cards = cards,
        basicLands = (basicLandsFallback ?: this).basicLands,
        incomplete = incomplete,
        sealedSupported = sealedSupported,
        block = block,
        releaseDate = releaseDate,
        boosterStrategy = boosterStrategy,
        printings = printings,
        variantChance = boosterVariantChance,
    )
