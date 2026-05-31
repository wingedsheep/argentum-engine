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
            register(set.cards.stamp(set.code).withLegalities())
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
    fun boosterGenerator(): BoosterGenerator = BoosterGenerator(
        activeSets()
            .filter { it.sealedSupported }
            .associate { it.code to it.toBoosterSetConfig() }
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

private fun List<CardDefinition>.stamp(setCode: String): List<CardDefinition> =
    map { if (it.setCode == null) it.copy(setCode = setCode) else it }

private fun List<CardDefinition>.withLegalities(): List<CardDefinition> =
    map { LegalityData.stamp(it) }

private fun MtgSet.toBoosterSetConfig(): BoosterGenerator.SetConfig =
    BoosterGenerator.SetConfig(
        setCode = code,
        setName = displayName,
        cards = cards,
        basicLands = (basicLandsFallback ?: this).basicLands,
        incomplete = incomplete,
        block = block,
        releaseDate = releaseDate,
        boosterStrategy = boosterStrategy,
    )
