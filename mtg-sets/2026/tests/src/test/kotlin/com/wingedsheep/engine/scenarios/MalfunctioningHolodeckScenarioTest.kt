package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.MalfunctioningHolodeck
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class MalfunctioningHolodeckScenarioTest : FunSpec({
    listOf(false, true).forEach { removeBeforeTrigger ->
        test("exile duration handles source removed ${if (removeBeforeTrigger) "before" else "after"} entry trigger resolves") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + MalfunctioningHolodeck)
            driver.initMirrorMatch(Deck.of("Plains" to 40))
            val you = driver.player1
            val opponent = driver.player2
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
            val holodeck = driver.putCardInHand(you, "Malfunctioning Holodeck")
            driver.giveMana(you, Color.WHITE, 3)
            driver.castSpell(you, holodeck)
            driver.bothPass()
            driver.submitTargetSelection(you, listOf(creature))
            if (!removeBeforeTrigger) {
                driver.bothPass()
                driver.getExile(opponent) shouldContain creature
            }
            // Its controller can remove the artifact without paying its ward cost.
            val removal = driver.putCardInHand(you, "Disenchant")
            driver.giveMana(you, Color.WHITE, 2)
            driver.castSpellWithTargets(you, removal, listOf(ChosenTarget.Permanent(holodeck)))
            driver.bothPass()
            // Returning the exiled permanent is part of the duration ending, with no return trigger.
            driver.state.getBattlefield().contains(creature) shouldBe true
            driver.state.getBattlefield().contains(holodeck) shouldBe false
            driver.stackSize shouldBe if (removeBeforeTrigger) 1 else 0
            if (removeBeforeTrigger) {
                driver.bothPass()
                driver.state.getBattlefield().contains(creature) shouldBe true
                driver.stackSize shouldBe 0
            }
        }
    }

    test("a blinked Holodeck cannot exile through its previous entry trigger") {
        val blink = card("Holodeck Test Blink") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell {
                val permanent = target(TargetFilter.PermanentYouControl)
                effect = Effects.Exile(permanent) then Effects.PutOntoBattlefield(permanent)
            }
        }
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MalfunctioningHolodeck, blink))
        driver.initMirrorMatch(Deck.of("Plains" to 40))
        val you = driver.player1
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val oldTarget = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val newTarget = driver.putCreatureOnBattlefield(opponent, "Hill Giant")
        val holodeck = driver.putCardInHand(you, "Malfunctioning Holodeck")
        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, holodeck)
        driver.bothPass()
        driver.submitTargetSelection(you, listOf(oldTarget))

        val blinkSpell = driver.putCardInHand(you, "Holodeck Test Blink")
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpellWithTargets(you, blinkSpell, listOf(ChosenTarget.Permanent(holodeck)))
        driver.bothPass()
        driver.submitTargetSelection(you, listOf(newTarget))
        driver.bothPass()
        driver.getExile(opponent) shouldContain newTarget
        driver.state.getBattlefield().contains(oldTarget) shouldBe true

        driver.bothPass()
        driver.state.getBattlefield().contains(oldTarget) shouldBe true
        driver.getExile(opponent) shouldContain newTarget
        driver.stackSize shouldBe 0
    }
})
