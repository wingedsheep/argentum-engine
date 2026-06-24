package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ether (FIN #53) — {3}{U} Artifact.
 *
 * "{T}, Exile this artifact: Add {U}. When you next cast an instant or sorcery spell this turn,
 *  copy that spell. You may choose new targets for the copy."
 *
 * The ability is a mana ability (it adds {U}, has no target, doesn't use the stack) that also sets
 * up a one-shot "copy your next instant/sorcery this turn" rider. Here, after activating it, casting
 * Shock (2 damage) at the opponent copies the spell, so the opponent takes 4 total.
 */
class EtherScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("activating Ether copies the next instant cast, doubling Shock's damage") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val ether = driver.putPermanentOnBattlefield(me, "Ether")
        driver.untapPermanent(ether)

        val abilityId = driver.cardRegistry.requireCard("Ether").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = ether, abilityId = abilityId)
        )

        val startingLife = driver.getLifeTotal(opp)

        // Cast Shock at the opponent (its {R} cost is paid from the {R} we give here).
        val shock = driver.putCardInHand(me, "Shock")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, shock, listOf(opp)).isSuccess shouldBe true

        // Resolve the copy (keeping the opponent as its target) and the original Shock.
        var guard = 0
        while (driver.stackSize > 0 && guard < 20) {
            if (driver.state.pendingDecision is ChooseTargetsDecision) {
                driver.submitTargetSelection(me, listOf(opp))
            } else {
                driver.bothPass()
            }
            guard++
        }

        // 2 (original Shock) + 2 (copy) = 4 damage to the opponent.
        driver.getLifeTotal(opp) shouldBe startingLife - 4
    }
})
