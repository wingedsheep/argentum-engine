package com.wingedsheep.gymserver.config

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.gym.service.MultiEnvService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires a default [CardRegistry] and [MultiEnvService] as Spring singletons.
 *
 * The chosen set catalogue here is deliberately small (Portal + Bloomburrow)
 * — enough to exercise the full service surface (constructed decks + sealed
 * pools). Extend via the `gym.sets` property once a richer set catalogue is
 * agreed on; sets are merged into both the card registry (for casting) and
 * the booster generator (for sealed).
 */
@Configuration
class GymBeansConfig {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
        register(BloomburrowSet.allCards)
        // Basic-land variants are needed for the RandomSealed path so that
        // variant names like "Swamp#BLB-270" resolve during GameInitializer.
        register(BloomburrowSet.basicLands)
    }

    @Bean
    fun boosterGenerator(): BoosterGenerator = BoosterGenerator(
        mapOf(
            BloomburrowSet.SET_CODE to BoosterGenerator.SetConfig(
                setCode = BloomburrowSet.SET_CODE,
                setName = BloomburrowSet.SET_NAME,
                cards = BloomburrowSet.allCards,
                basicLands = BloomburrowSet.basicLands
            )
        )
    )

    @Bean
    fun multiEnvService(
        cardRegistry: CardRegistry,
        boosterGenerator: BoosterGenerator
    ): MultiEnvService = MultiEnvService(cardRegistry, boosterGenerator)
}
