package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.FlareOfDuplication
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flare of Duplication {1}{R}{R} — Instant (MH3).
 *
 * "You may sacrifice a nontoken red creature rather than pay this spell's mana cost.
 *  Copy target instant or sorcery spell. You may choose new targets for the copy."
 */
class FlareOfDuplicationScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FlareOfDuplication))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castBolt(driver: GameTestDriver, caster: EntityId, at: EntityId): EntityId {
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Player(at))).error shouldBe null
        return bolt
    }

    fun resolveRedirectingCopyTo(driver: GameTestDriver, chooser: EntityId, newTarget: EntityId) {
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(chooser, listOf(newTarget))
        guard = 0
        while (driver.stackSize > 0 && guard < 20) {
            driver.bothPass()
            guard++
        }
    }

    test("sacrificing a nontoken red creature copies target spell for free, copy redirected") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val blade = driver.putCreatureOnBattlefield(me, "Blade of the Ninth Watch")
        val flare = driver.putCardInHand(me, "Flare of Duplication")
        // My own bolt aimed at me; the copy is redirected at the opponent.
        val bolt = castBolt(driver, me, me)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = flare,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(blade)),
            )
        ).error shouldBe null
        withClue("the red creature paid the alternative cost") {
            driver.findPermanent(me, "Blade of the Ninth Watch") shouldBe null
        }

        resolveRedirectingCopyTo(driver, me, opponent)
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(me) shouldBe 17
    }

    test("hard-cast for its mana cost copies target spell") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val flare = driver.putCardInHand(me, "Flare of Duplication")
        val bolt = castBolt(driver, me, opponent)
        driver.giveMana(me, Color.RED, 3)
        driver.castSpellWithTargets(me, flare, listOf(ChosenTarget.Spell(bolt))).error shouldBe null

        resolveRedirectingCopyTo(driver, me, opponent)
        driver.getLifeTotal(opponent) shouldBe 14
    }

    test("a non-red creature can't pay the alternative cost") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val black = driver.putCreatureOnBattlefield(me, "Black Creature")
        val flare = driver.putCardInHand(me, "Flare of Duplication")
        val bolt = castBolt(driver, me, opponent)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = flare,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(black)),
            )
        ).error shouldNotBe null
        driver.findPermanent(me, "Black Creature") shouldNotBe null
    }
})
