package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.DazzlingReflection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dazzling Reflection (OGW #17).
 *
 * You gain life equal to target creature's power. The next time that creature would deal damage
 * this turn, prevent that damage.
 *
 * Cast on an attacker: the life is gained as the spell resolves (per its rulings, not as the
 * damage is prevented), and the attacker's combat damage is prevented, spending the shield.
 */
class DazzlingReflectionScenarioTest : FunSpec({

    test("gains life equal to the attacker's power and prevents its combat damage") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DazzlingReflection))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val courser = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.removeSummoningSickness(courser)
        val reflection = driver.putCardInHand(defender, "Dazzling Reflection")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(courser), defender).error shouldBe null
        driver.passPriority(attacker)
        driver.giveMana(defender, Color.WHITE, 2)
        driver.castSpellWithTargets(defender, reflection, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        driver.bothPass()

        driver.assertLifeTotal(defender, 23)
        driver.state.floatingEffects.count {
            it.effect.modification is SerializableModification.PreventNextDamageFromSourceShield
        } shouldBe 1

        driver.bothPass()
        driver.declareNoBlockers(defender).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(defender, 23)
        driver.state.floatingEffects.count {
            it.effect.modification is SerializableModification.PreventNextDamageFromSourceShield
        } shouldBe 0
    }
})
