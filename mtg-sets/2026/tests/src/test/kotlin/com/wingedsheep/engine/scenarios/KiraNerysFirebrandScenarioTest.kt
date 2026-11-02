package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kira Nerys, Firebrand (TRK #242) — {1}{R}{W} 4/3 trample.
 *
 * "If a creature you control would enter while you control an attacking creature, instead it enters
 * tapped and attacking. Whenever another creature you control enters, it gets +1/+1 until end of turn."
 *
 * The replacement's rules matrix (every entry seam, the CR 508.4 defender choice, opponents' creatures)
 * lives in the engine's `PermanentsEnterTappedAndAttackingTest`; this pins the card's own wiring.
 */
class KiraNerysFirebrandScenarioTest : ScenarioTestBase() {

    init {
        context("Kira Nerys, Firebrand") {

            test("a flashed-in creature joins the attack and gets +1/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kira Nerys, Firebrand")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Ambush Viper")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Kira Nerys, Firebrand" to 2)).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val viper = game.findPermanent("Ambush Viper")!!
                withClue("the Viper enters tapped and attacking player 2") {
                    game.state.getEntity(viper)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(viper)?.get<AttackingComponent>()?.defenderId shouldBe game.player2Id
                }
                withClue("Kira's enter trigger pumps the Viper to 3/2") {
                    game.state.projectedState.getPower(viper) shouldBe 3
                    game.state.projectedState.getToughness(viper) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                withClue("Kira (4) and the pumped Viper (3) both connect") {
                    game.getLifeTotal(2) shouldBe 13
                }
            }

            test("a creature entering outside combat enters normally but is still pumped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kira Nerys, Firebrand")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Ambush Viper")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ambush Viper").error shouldBe null
                game.resolveStack()

                val viper = game.findPermanent("Ambush Viper")!!
                game.state.getEntity(viper)?.has<TappedComponent>() shouldBe false
                game.state.getEntity(viper)?.get<AttackingComponent>() shouldBe null
                game.state.projectedState.getPower(viper) shouldBe 3
            }
        }
    }
}
