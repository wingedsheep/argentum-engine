package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SkillfulLungeScenarioTest : FunSpec({

    test("target creature gets +2/+0 and gains first strike") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val spell = driver.putCardInHand(you, "Skillful Lunge")
        driver.giveMana(you, Color.WHITE, 2)
        driver.castSpell(you, spell, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bears) shouldBe 4
        projected.getToughness(bears) shouldBe 2
        projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
    }
})
