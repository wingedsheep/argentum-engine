package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameBeansConfig(
    private val gameProperties: GameProperties
) {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
        register(PortalSet.basicLands)
        if (gameProperties.sets.onslaughtEnabled) {
            register(OnslaughtSet.allCards)
        }
    }

    @Bean
    fun boosterGenerator(): BoosterGenerator {
        val sets = buildMap {
            put(PortalSet.SET_CODE, BoosterGenerator.portalSetConfig)
            if (gameProperties.sets.onslaughtEnabled) {
                put(OnslaughtSet.SET_CODE, BoosterGenerator.onslaughtSetConfig)
            }
        }
        return BoosterGenerator(sets)
    }

    @Bean
    fun randomDeckGenerator(): RandomDeckGenerator = RandomDeckGenerator(
        cardPool = OnslaughtSet.allCards,
        basicLandVariants = PortalSet.basicLands
    )
}
