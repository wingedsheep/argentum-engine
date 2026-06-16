package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Eye for an Eye (Arabian Nights, {W}{W} Instant).
 *
 * Oracle: "The next time a source of your choice would deal damage to you this turn, instead that
 * source deals that much damage to you and Eye for an Eye deals that much damage to that source's
 * controller."
 *
 * Verifies the `preventDamage = false` reflection path: the chosen source still deals its damage to
 * the caster (it is NOT prevented), and an equal amount is reflected to that source's controller.
 */
class EyeForAnEyeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("a chosen attacker's combat damage still hits the caster and is also dealt to its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val caster = driver.activePlayer!!          // casts Eye for an Eye and gets attacked
        val attacker = driver.getOpponent(caster)   // attacks with Grizzly Bears (its controller)

        val eye = driver.putCardInHand(caster, "Eye for an Eye")
        driver.putLandOnBattlefield(caster, "Plains")
        driver.putLandOnBattlefield(caster, "Plains")
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(bears)

        // Advance to the attacker's declare-attackers step.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        if (driver.activePlayer != attacker) {
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        }
        driver.declareAttackers(attacker, listOf(bears), caster)

        // In response, the caster casts Eye for an Eye and names Grizzly Bears as the source.
        driver.passPriority(attacker)
        driver.castSpell(caster, eye)
        driver.bothPass() // resolve the spell — pauses for the source choice
        val decisionId = driver.pendingDecision!!.id
        driver.submitDecision(caster, CardsSelectedResponse(decisionId, listOf(bears)))

        // Resolve through combat damage and the reflected-damage trigger.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(caster, 18)   // the 2 combat damage is NOT prevented — caster still takes it
        driver.assertLifeTotal(attacker, 18) // and Eye for an Eye reflects 2 to Grizzly Bears' controller
        driver.findPermanent(attacker, "Grizzly Bears") shouldNotBe null // unblocked attacker survives
    }

    // Combat damage above flows through CombatDamageManager; non-combat damage (burn spells,
    // abilities) flows through the separate DamageUtils.applyDamage path. This exercises that
    // second path's reflect branch so both call sites of the shared shield are covered.
    test("a chosen direct-damage spell still hits the caster and is reflected to its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)

        val caster = driver.activePlayer!!          // casts Eye for an Eye
        val opponent = driver.getOpponent(caster)   // casts Lightning Bolt (its controller)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val eye = driver.putCardInHand(caster, "Eye for an Eye")
        driver.putLandOnBattlefield(caster, "Plains")
        driver.putLandOnBattlefield(caster, "Plains")

        // Opponent holds a Lightning Bolt (3 damage to any target) to point at the caster.
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")

        // Caster passes priority so the opponent can cast Lightning Bolt at the caster.
        driver.passPriority(caster)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(caster))).error shouldBe null

        // With the Bolt on the stack, the opponent passes priority back and the caster responds with
        // Eye for an Eye, naming the Bolt.
        driver.passPriority(opponent)
        driver.castSpell(caster, eye)
        driver.bothPass() // resolve Eye for an Eye — pauses for the source choice
        val decisionId = driver.pendingDecision!!.id
        driver.submitDecision(caster, CardsSelectedResponse(decisionId, listOf(bolt)))

        // Resolve the Bolt (DamageUtils.applyDamage path) and the reflected-damage trigger.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(caster, 17)   // the 3 direct damage is NOT prevented — caster still takes it
        driver.assertLifeTotal(opponent, 17) // and Eye for an Eye reflects 3 to the Bolt's controller
    }
})
