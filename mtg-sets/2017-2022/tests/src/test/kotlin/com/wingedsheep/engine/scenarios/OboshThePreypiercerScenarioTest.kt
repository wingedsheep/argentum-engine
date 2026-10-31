package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Obosh, the Preypiercer (IKO #228) — "If a source you control with an odd mana value would deal
 * damage to a permanent or player, it deals double that damage to that permanent or player instead."
 *
 * The parity is read off the source when the damage is dealt: a spell's announced X counts on the
 * stack, an even-mana-value source and an opponent's odd source are untouched.
 */
class OboshThePreypiercerScenarioTest : ScenarioTestBase() {

    private fun board(block: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Obosh, the Preypiercer")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .block()
        .build()

    init {
        test("an odd-mana-value spell you control deals double damage") {
            val game = board {
                withCardInHand(1, "Lightning Bolt").withLandsOnBattlefield(1, "Mountain", 1)
            }
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 14
        }

        test("a spell's announced X decides its parity on the stack") {
            val odd = board {
                withCardInHand(1, "Blaze").withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
            }
            withClue("Blaze with X=2 has mana value 3 — doubled to 4, killing the 3/3 Hill Giant") {
                odd.castXSpell(1, "Blaze", 2, odd.findPermanent("Hill Giant")!!).error shouldBe null
                odd.resolveStack()
                odd.findPermanent("Hill Giant") shouldBe null
            }

            val even = board {
                withCardInHand(1, "Blaze").withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
            }
            withClue("Blaze with X=1 has mana value 2 — not doubled, so the 2/2 Grizzly Bears survives") {
                even.castXSpell(1, "Blaze", 1, even.findPermanent("Grizzly Bears")!!).error shouldBe null
                even.resolveStack()
                even.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }

        test("combat damage: an odd creature doubles, an even creature doesn't") {
            val game = board {
                withCardOnBattlefield(1, "Gray Ogre", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Gray Ogre" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("Gray Ogre (MV 3) deals 4, Grizzly Bears (MV 2) deals 2") {
                game.getLifeTotal(2) shouldBe 14
            }
        }

        test("an opponent's odd-mana-value source is not doubled") {
            val game = board {
                withCardInHand(2, "Lightning Bolt").withLandsOnBattlefield(2, "Mountain", 1)
            }
            game.passPriority()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }
    }
}
