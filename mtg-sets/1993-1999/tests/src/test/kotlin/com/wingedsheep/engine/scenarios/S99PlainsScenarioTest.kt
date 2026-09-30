package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.s99.Starter1999Set
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Plains154
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Plains155
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Plains156
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Plains157
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class S99PlainsScenarioTest : FunSpec({
    val variants = listOf(
        Starter1999Plains154,
        Starter1999Plains155,
        Starter1999Plains156,
        Starter1999Plains157
    )

    test("set discovery exposes all four Starter 1999 Plains arts") {
        val discovered = Starter1999Set.basicLands.filter { it.name == "Plains" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("154", "155", "156", "157")
        discovered.size shouldBe 4
        discovered.forEach { it.setCode shouldBe "S99" }
    }

    variants.forEach { plains ->
        test("Starter 1999 Plains ${plains.metadata.collectorNumber} taps for white mana immediately") {
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
