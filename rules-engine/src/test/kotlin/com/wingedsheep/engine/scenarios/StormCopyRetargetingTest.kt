package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 702.40a: "you may choose new targets for any of the copies." Verify that
 * the retargeting prompt fires for Storm copies of a targeted spell and that
 * the chosen new target lands on the copy's [TargetsComponent].
 */
class StormCopyRetargetingTest : FunSpec({

    test("Storm copy retargets to a different player than the original") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")

        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        // Pass priority so the Storm trigger on top of the stack resolves.
        // Expect the executor to pause with a ChooseTargetsDecision for the sole copy.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        // Submit the caster (self) as the new target for the Storm copy.
        driver.submitTargetSelection(caster, listOf(caster)).isSuccess shouldBe true

        // A copy of Tendrils must now be on the stack with caster as its chosen target.
        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        val copyTargets = driver.state.getEntity(copyId)!!.get<TargetsComponent>()
        copyTargets?.targets shouldBe listOf(ChosenTarget.Player(caster))
    }

    test("Storm copy may keep the original target (picking the same player is legal)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        driver.submitTargetSelection(caster, listOf(opponent)).isSuccess shouldBe true

        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        val copyTargets = driver.state.getEntity(copyId)!!.get<TargetsComponent>()
        copyTargets?.targets shouldBe listOf(ChosenTarget.Player(opponent))
    }
})
