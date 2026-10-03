package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class WurmcoilLarvaScenarioTest : ScenarioTestBase() {

    init {
        test("Wurmcoil Larva has deathtouch and lifelink") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Wurmcoil Larva")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val larva = game.findPermanent("Wurmcoil Larva")!!
            val projected = game.state.projectedState
            projected.hasKeyword(larva, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(larva, Keyword.LIFELINK) shouldBe true
            projected.hasType(larva, "ARTIFACT") shouldBe true
        }

        test("when it dies, it creates a 1/2 deathtouch and a 2/1 lifelink black Phyrexian Wurm artifact token") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Wurmcoil Larva")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val larva = game.findPermanent("Wurmcoil Larva")!!

            game.castSpell(1, "Lightning Bolt", larva).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Wurmcoil Larva") shouldBe true
            val tokens = game.state.getBattlefield().filter { game.state.getEntity(it)?.has<TokenComponent>() == true }
            tokens shouldHaveSize 2
            val projected = game.state.projectedState
            tokens.forEach { token ->
                projected.isCreature(token) shouldBe true
                projected.hasType(token, "ARTIFACT") shouldBe true
                projected.hasSubtype(token, "Phyrexian") shouldBe true
                projected.hasSubtype(token, "Wurm") shouldBe true
                projected.getColors(token) shouldHaveSize 1
                projected.hasColor(token, Color.BLACK) shouldBe true
                game.state.getBattlefield().contains(token) shouldBe true
            }
            val deathtouch = tokens.single { projected.getPower(it) == 1 }
            projected.getToughness(deathtouch) shouldBe 2
            projected.hasKeyword(deathtouch, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(deathtouch, Keyword.LIFELINK) shouldBe false

            val lifelink = tokens.single { projected.getPower(it) == 2 }
            projected.getToughness(lifelink) shouldBe 1
            projected.hasKeyword(lifelink, Keyword.LIFELINK) shouldBe true
            projected.hasKeyword(lifelink, Keyword.DEATHTOUCH) shouldBe false
        }
    }
}
