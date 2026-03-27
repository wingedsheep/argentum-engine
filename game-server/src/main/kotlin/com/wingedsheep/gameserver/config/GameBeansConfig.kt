package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.edgeofeternities.EdgeOfEternitiesSet
import com.wingedsheep.mtg.sets.definitions.dominaria.DominariaSet
import com.wingedsheep.mtg.sets.definitions.khans.KhansOfTarkirSet
import com.wingedsheep.mtg.sets.definitions.legions.LegionsSet
import com.wingedsheep.mtg.sets.definitions.onslaught.OnslaughtSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
import com.wingedsheep.mtg.sets.definitions.custom.JustOneGlassToken
import com.wingedsheep.mtg.sets.definitions.custom.SekshaasEarlySleeper
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameBeansConfig(
    private val gameProperties: GameProperties
) {

    @Bean
    fun cardRegistry(): CardRegistry = CardRegistry().apply {
        register(PredefinedTokens.allTokens)
        register(PortalSet.allCards)
        register(PortalSet.basicLands)
        if (gameProperties.sets.onslaughtEnabled) {
            register(OnslaughtSet.allCards)
        }
        if (gameProperties.sets.scourgeEnabled) {
            register(ScourgeSet.allCards)
        }
        if (gameProperties.sets.legionsEnabled) {
            register(LegionsSet.allCards)
        }
        if (gameProperties.sets.khansEnabled) {
            register(KhansOfTarkirSet.allCards)
        }
        if (gameProperties.sets.dominariaEnabled) {
            register(DominariaSet.allCards)
        }
        if (gameProperties.sets.bloomburrowEnabled) {
            register(BloomburrowSet.allCards)
        }
        if (gameProperties.sets.edgeOfEternitiesEnabled) {
            register(EdgeOfEternitiesSet.allCards)
        }
        // Easter egg card — injected into Rick's deck at game start
        register(SekshaasEarlySleeper)
        register(JustOneGlassToken)
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
            if (gameProperties.sets.legionsEnabled) {
                put(LegionsSet.SET_CODE, BoosterGenerator.legionsSetConfig)
            }
            if (gameProperties.sets.khansEnabled) {
                put(KhansOfTarkirSet.SET_CODE, BoosterGenerator.khansSetConfig)
            }
            if (gameProperties.sets.dominariaEnabled) {
                put(DominariaSet.SET_CODE, BoosterGenerator.dominariaSetConfig)
            }
            if (gameProperties.sets.bloomburrowEnabled) {
                put(BloomburrowSet.SET_CODE, BoosterGenerator.bloomburrowSetConfig)
            }
            if (gameProperties.sets.edgeOfEternitiesEnabled) {
                put(EdgeOfEternitiesSet.SET_CODE, BoosterGenerator.edgeOfEternitiesSetConfig)
            }
        }
        return BoosterGenerator(sets)
    }

    @Bean
    fun randomDeckGenerator(): RandomDeckGenerator = RandomDeckGenerator(
        cardPool = buildList {
            if (gameProperties.sets.onslaughtEnabled) addAll(OnslaughtSet.allCards)
            if (gameProperties.sets.scourgeEnabled) addAll(ScourgeSet.allCards)
            if (gameProperties.sets.legionsEnabled) addAll(LegionsSet.allCards)
            if (gameProperties.sets.khansEnabled) addAll(KhansOfTarkirSet.allCards)
            if (gameProperties.sets.dominariaEnabled) addAll(DominariaSet.allCards)
            if (gameProperties.sets.bloomburrowEnabled) addAll(BloomburrowSet.allCards)
            if (gameProperties.sets.edgeOfEternitiesEnabled) addAll(EdgeOfEternitiesSet.allCards)
        },
        basicLandVariants = PortalSet.basicLands,
        setCodes = buildList {
            if (gameProperties.sets.onslaughtEnabled) add("ONS")
            if (gameProperties.sets.scourgeEnabled) add("SCG")
            if (gameProperties.sets.legionsEnabled) add("LGN")
            if (gameProperties.sets.khansEnabled) add("KTK")
            if (gameProperties.sets.dominariaEnabled) add("DOM")
            if (gameProperties.sets.bloomburrowEnabled) add("BLB")
            if (gameProperties.sets.edgeOfEternitiesEnabled) add("EOE")
        }
    )
}
