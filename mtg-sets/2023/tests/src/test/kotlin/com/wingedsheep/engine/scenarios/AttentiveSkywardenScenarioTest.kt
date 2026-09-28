package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Attentive Skywarden (MOM #7) — "Flying. Whenever this creature deals combat damage to a player
 * or battle, transform up to one target Incubator token you control."
 *
 * The Incubator comes from Norn's Inquisitor's incubate 2.
 */
class AttentiveSkywardenScenarioTest : ScenarioTestBase() {

    private fun TestGame.setUpWithIncubator(): TestGame {
        castSpell(1, "Norn's Inquisitor")
        resolveStack()
        findPermanent("Incubator") shouldNotBe null
        return this
    }

    private fun board(blocker: String? = null) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Attentive Skywarden", summoningSickness = false)
        .withCardInHand(1, "Norn's Inquisitor")
        .withLandsOnBattlefield(1, "Plains", 4)
        .apply { if (blocker != null) withCardOnBattlefield(2, blocker, summoningSickness = false) }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .setUpWithIncubator()

    private fun TestGame.attackPlayer() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Attentive Skywarden" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    init {
        context("Attentive Skywarden") {

            test("combat damage to a player transforms the targeted Incubator token") {
                val game = board()
                val incubator = game.findPermanent("Incubator")!!
                game.attackPlayer()

                withClue("Skywarden dealt 2 to the opponent") { game.getLifeTotal(2) shouldBe 18 }
                game.selectTargets(listOf(incubator)).error shouldBe null
                game.resolveStack()

                withClue("the Incubator flipped to its Phyrexian face") {
                    game.findPermanent("Incubator") shouldBe null
                    game.findPermanent("Phyrexian") shouldBe incubator
                }
            }

            test("choosing no target leaves the Incubator untransformed") {
                val game = board()
                val incubator = game.findPermanent("Incubator")!!
                game.attackPlayer()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.findPermanent("Incubator") shouldBe incubator
                game.findPermanent("Phyrexian") shouldBe null
            }

            test("blocked by a flyer — no combat damage to a player, no trigger") {
                val game = board(blocker = "Wind Drake")
                val incubator = game.findPermanent("Incubator")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Attentive Skywarden" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wind Drake" to listOf("Attentive Skywarden"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.getLifeTotal(2) shouldBe 20
                game.state.pendingDecision shouldBe null
                game.findPermanent("Incubator") shouldBe incubator
            }
        }
    }
}
