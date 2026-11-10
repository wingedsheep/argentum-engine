package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pyromancer's Assault — {3}{R} Enchantment.
 * "Whenever you cast your second spell each turn, this enchantment deals 2 damage to any target."
 */
class PyromancersAssaultScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Pyromancer's Assault")
            .withCardsInHand(1, "Grizzly Bears", 2)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the first spell doesn't trigger; the second deals 2 damage to the chosen player") {
            val game = board()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("no trigger on the first spell") {
                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("the second spell's trigger can hit a creature") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("2 damage on a 3/3 leaves it alive but damaged") {
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            }
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
