package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.FesteringGoblin
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression: when a player loses the game (life 0) in the same SBA pass that
 * one of their creatures goes to the graveyard, the dying creature's targeted
 * dies-trigger must not pause the game asking the just-lost player to choose
 * a target. Previously the trigger would pause and the game would deadlock:
 * ActionProcessor rejects any SubmitDecision once gameOver is true, and the
 * tournament flow never saw a clean GameOver message.
 */
class DyingPlayerDiesTriggerTest : FunSpec({

    test("dies-trigger controlled by a losing player does not pause the game") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FesteringGoblin))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val attacker = driver.player1
        val defender = driver.player2

        // Defender at 1 life and controls Festering Goblin (1/1) whose dies
        // trigger targets any creature — crucially, legal targets still exist
        // after the goblin dies (the attacker's surviving creatures), so the
        // trigger would pause for target selection rather than fizzling.
        driver.replaceState(
            driver.state.updateEntity(defender) { c -> c.with(LifeTotalComponent(1)) }
        )
        val goblin = driver.putCreatureOnBattlefield(defender, "Festering Goblin")
        driver.removeSummoningSickness(goblin)

        // Attacker brings two 1/1s. One will be blocked by the goblin (both
        // die to 1 damage each), the other hits the face for lethal.
        val lion1 = driver.putCreatureOnBattlefield(attacker, "Savannah Lions")
        val lion2 = driver.putCreatureOnBattlefield(attacker, "Savannah Lions")
        driver.removeSummoningSickness(lion1)
        driver.removeSummoningSickness(lion2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, mapOf(lion1 to defender, lion2 to defender))
        driver.bothPass()

        // DeclareBlockers map is blocker -> attackers (not the other way around).
        driver.declareBlockers(defender, mapOf(goblin to listOf(lion1)))
        driver.bothPass()

        // Defender lost and the game is over. Critically, there must be no
        // lingering decision waiting on the defender to pick a target for
        // Festering Goblin's dies trigger — despite lion2 being a legal target,
        // the trigger's controller (defender) has lost the game and per CR
        // 704.6 / 800.4a the trigger is dropped rather than resolving.
        driver.state.gameOver shouldBe true
        driver.state.getEntity(defender)?.has<PlayerLostComponent>() shouldBe true
        driver.pendingDecision shouldBe null
    }
})
