package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SilverclawGriffinScenarioTest : FunSpec({

    test("it is a 3/2 with flying and first strike") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val griffin = driver.putCreatureOnBattlefield(you, "Silverclaw Griffin")

        val projected = driver.state.projectedState
        projected.getPower(griffin) shouldBe 3
        projected.getToughness(griffin) shouldBe 2
        projected.hasKeyword(griffin, Keyword.FLYING) shouldBe true
        projected.hasKeyword(griffin, Keyword.FIRST_STRIKE) shouldBe true
    }
})
