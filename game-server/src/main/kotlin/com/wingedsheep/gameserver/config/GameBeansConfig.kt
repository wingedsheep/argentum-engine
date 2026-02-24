package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.khans.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
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
        if (gameProperties.sets.scourgeEnabled) {
            register(ScourgeSet.allCards)
        }
        if (gameProperties.sets.khansEnabled) {
            register(KhansOfTarkirSet.allCards)
        }
    }

    @Bean
    fun boosterGenerator(): BoosterGenerator {
        val sets = buildMap {
            put(PortalSet.SET_CODE, BoosterGenerator.portalSetConfig)
            if (gameProperties.sets.onslaughtEnabled) {
                put(OnslaughtSet.SET_CODE, BoosterGenerator.onslaughtSetConfig)
            }
            if (gameProperties.sets.scourgeEnabled) {
                put(ScourgeSet.SET_CODE, BoosterGenerator.scourgeSetConfig)
            }
            if (gameProperties.sets.khansEnabled) {
                put(KhansOfTarkirSet.SET_CODE, BoosterGenerator.khansSetConfig)
            }
        }
        return BoosterGenerator(sets)
    }

    @Bean
    fun randomDeckGenerator(): RandomDeckGenerator = RandomDeckGenerator(
//        TODO: Enable once onslaught is implemented
        cardPool = if (gameProperties.sets.onslaughtEnabled) {
            /*PortalSet.allCards + */ OnslaughtSet.allCards
        } else {
            PortalSet.allCards
        },
//        cardPool = PortalSet.allCards,
        basicLandVariants = PortalSet.basicLands
    )
}
