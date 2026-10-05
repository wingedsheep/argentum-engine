package com.wingedsheep.gym.server.config

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gym.service.MultiEnvService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.basicLandsFallback
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires a default [CardRegistry] and [MultiEnvService] as Spring singletons.
 *
 * The catalogue is [MtgSetCatalog.all] — every set the engine knows about — so a
 * constructed deck can name any implemented card and a [DeckSpec.RandomSealed] can
 * draw from any sealed-supported set. This mirrors `game-server`'s `GameBeansConfig`;
 * `MtgSetCatalog` is the single registration point, so newly-added sets show up here
 * automatically with no edit to this file.
 */
@Configuration
class GymBeansConfig {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        // Predefined tokens (Treasure, Food, Clue, Map, Incubator, …) are looked up by name at
        // resolution time by `CreatePredefinedTokenExecutor`, which returns an `EffectResult.error`
        // when the name is unregistered — so without this line every card in the corpus that mints
        // one silently minted nothing over the gym API, while `game-server`'s `GameBeansConfig`
        // (which does register them) behaved correctly. Self-play then reported those cards as broken.
        register(PredefinedTokens.allTokens)
        for (set in MtgSetCatalog.all) {
            register(set.cards.stampSetCode(set.code))
            // Basic-land variants are needed for the RandomSealed path so that
            // variant names like "Swamp#BLB-270" resolve during GameInitializer.
            register(set.basicLands)
            set.basicLandsFallback?.let { register(it.basicLands) }
        }
    }

    @Bean
    fun printingRegistry(cardRegistry: CardRegistry): PrintingRegistry = PrintingRegistry().apply {
        // One synthesised printing per registered card (its canonical printing)...
        for (name in cardRegistry.allCardNames()) {
            cardRegistry.getCardsByName(name).forEach(::registerSynthesizedDefault)
        }
        // ...then explicit reprint rows, which overwrite synthesised entries sharing a key.
        for (set in MtgSetCatalog.all) {
            register(set.printings)
        }
    }

    @Bean
    fun boosterGenerator(): BoosterGenerator = BoosterGenerator(
        MtgSetCatalog.all
            .filter { it.sealedSupported }
            .associate { it.code to it.toBoosterSetConfig() }
    )

    @Bean
    fun multiEnvService(
        cardRegistry: CardRegistry,
        boosterGenerator: BoosterGenerator
    ): MultiEnvService = MultiEnvService(cardRegistry, boosterGenerator)
}

/** Stamp a set code onto any card that doesn't already carry one, so printing synthesis keys correctly. */
private fun List<CardDefinition>.stampSetCode(setCode: String): List<CardDefinition> =
    map { if (it.setCode == null) it.copy(setCode = setCode) else it }

private fun MtgSet.toBoosterSetConfig(): BoosterGenerator.SetConfig =
    BoosterGenerator.SetConfig(
        setCode = code,
        setName = displayName,
        cards = cards,
        basicLands = (basicLandsFallback ?: this).basicLands,
        incomplete = incomplete,
        block = block,
        boosterStrategy = boosterStrategy,
    )
