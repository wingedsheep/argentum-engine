package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Savage Ventmaw — "{4}{R}{G} 4/4 Flying Dragon. Whenever this creature attacks, add
 * {R}{R}{R}{G}{G}{G}." (On this engine, mana persists until the end-of-turn pool emptying, which
 * is exactly the "you don't lose this mana as steps and phases end. Until end of turn" clause.)
 */
class SavageVentmawScenarioTest : FunSpec({

    test("attacking adds three red and three green to the controller's pool") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)

        val attacker = d.activePlayer!!
        val defender = d.getOpponent(attacker)

        val ventmaw = d.putCreatureOnBattlefield(attacker, "Savage Ventmaw")
        d.removeSummoningSickness(ventmaw)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(attacker, listOf(ventmaw), defender)
        d.bothPass() // the attack trigger resolves, adding the mana

        val pool = d.state.getEntity(attacker)?.get<ManaPoolComponent>()!!
        pool.red shouldBe 3
        pool.green shouldBe 3
    }
})
