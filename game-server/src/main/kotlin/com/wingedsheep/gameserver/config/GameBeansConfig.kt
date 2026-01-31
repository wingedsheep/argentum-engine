package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.lorwyn.LorwynEclipsedSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameBeansConfig {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
        register(PortalSet.basicLands)
        register(LorwynEclipsedSet.allCards)
    }

    @Bean
    fun randomDeckGenerator(): RandomDeckGenerator = RandomDeckGenerator(
        cardPool = PortalSet.allCards,
        basicLandVariants = PortalSet.basicLands
    )
}
