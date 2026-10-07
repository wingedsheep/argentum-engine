package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.ulg.cards.TreetopVillage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TreetopVillageScenarioTest : FunSpec({
    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(TreetopVillage)
        it.initMirrorMatch(deck = Deck.of("Forest" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("playing the land enters tapped") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val village = driver.putCardInHand(player, "Treetop Village")
        driver.submitSuccess(PlayLand(player, village))
        driver.isTapped(village) shouldBe true
        driver.submitExpectFailure(ActivateAbility(player, village, TreetopVillage.activatedAbilities[0].id))
    }

    test("animation keeps the land and mana ability and expires at cleanup") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val village = driver.putLandOnBattlefield(player, "Treetop Village")
        driver.giveMana(player, Color.GREEN, 2)
        driver.submitSuccess(ActivateAbility(player, village, TreetopVillage.activatedAbilities[1].id))
        driver.bothPass()

        val projected = StateProjector().project(driver.state)
        projected.hasType(village, "LAND") shouldBe true
        projected.hasType(village, "CREATURE") shouldBe true
        projected.hasSubtype(village, "Ape") shouldBe true
        projected.getPower(village) shouldBe 3
        projected.getToughness(village) shouldBe 3
        projected.getColors(village) shouldBe setOf("GREEN")
        projected.hasKeyword(village, Keyword.TRAMPLE) shouldBe true
        driver.submitSuccess(ActivateAbility(player, village, TreetopVillage.activatedAbilities[0].id))
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1

        driver.passPriorityUntil(Step.UPKEEP)
        val next = StateProjector().project(driver.state)
        next.hasType(village, "LAND") shouldBe true
        next.hasType(village, "CREATURE") shouldBe false
        next.hasSubtype(village, "Ape") shouldBe false
        next.hasKeyword(village, Keyword.TRAMPLE) shouldBe false
        next.getColors(village) shouldBe emptySet()
    }
})
