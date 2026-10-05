package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaPlains286
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaPlains287
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaPlainsScenarioTest : FunSpec({
    val variants = listOf(
        AlphaPlains286,
        AlphaPlains287
    )

    test("set discovery exposes both Alpha Plains arts") {
        val discovered = AlphaSet.basicLands.filter { it.name == "Plains" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("286", "287")
        discovered.size shouldBe 2
        discovered.forEach { it.setCode shouldBe "LEA" }
    }

    variants.forEach { plains ->
        test("Alpha Plains ${plains.metadata.collectorNumber} taps for white mana immediately") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all.filter { it.name != "Plains" })
            driver.registerCard(plains)
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val player = driver.activePlayer!!
            val land = driver.putPermanentOnBattlefield(player, "Plains")
            val result = driver.submit(
                ActivateAbility(player, land, plains.activatedAbilities.single().id)
            )

            result.outcome shouldBe Outcome.Done
            driver.isTapped(land) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.white shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
        }
    }
})
