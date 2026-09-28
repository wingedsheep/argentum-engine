package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.KongmingsContraptions
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KongmingsContraptionsScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KongmingsContraptions))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("deals 2 damage to an attacking creature when its controller is attacked") {
        val driver = newGame()
        val attackerPlayer = driver.activePlayer!!
        val me = driver.getOpponent(attackerPlayer)
        val bears = driver.putCreatureOnBattlefield(attackerPlayer, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val contraptions = driver.putCreatureOnBattlefield(me, "Kongming's Contraptions")
        driver.removeSummoningSickness(contraptions)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attackerPlayer, listOf(bears), defendingPlayer = me).error shouldBe null

        val abilityId = KongmingsContraptions.activatedAbilities[0].id
        driver.passPriority(attackerPlayer)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = contraptions, abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()
        driver.state.getBattlefield().contains(bears) shouldBe false
    }

    test("cannot be activated outside the declare attackers step") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val other = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(other, "Grizzly Bears")
        val contraptions = driver.putCreatureOnBattlefield(me, "Kongming's Contraptions")
        driver.removeSummoningSickness(contraptions)
        val result = driver.submit(
            ActivateAbility(
                playerId = me, sourceId = contraptions,
                abilityId = KongmingsContraptions.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        result.error shouldNotBe null
    }
})
