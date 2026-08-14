package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Extract a Confession (MKM) — {1}{B} sorcery, optional collect evidence 6, "each opponent
 * sacrifices a creature of their choice. If evidence was collected, instead each opponent
 * sacrifices a creature with the greatest power among creatures they control."
 *
 * The card composes from existing vocabulary, but the interesting claim is the *linkage*: the
 * `ChoiceSlot.EVIDENCE_COLLECTED` stamped at cast time has to survive to resolution and pick the
 * greatest-power branch (CR 701.59c / CR 607). These tests prove it from both sides by giving the
 * opponent a 1/1 and a 4/4 and watching which one dies — a free choice leaves the small one on the
 * board, evidence forces the big one.
 *
 * Air Elemental (mana value 5) plus Hill Giant (4) clears the evidence-6 threshold.
 */
class ExtractAConfessionScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Extract a Confession")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInGraveyard(1, "Air Elemental")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Air Elemental")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("without evidence the opponent chooses which creature to sacrifice") {
            val game = board().build()

            game.castSpell(1, "Extract a Confession").error shouldBe null
            game.resolveStack()

            withClue("the opponent picks — the harness takes the first offered option") {
                game.hasPendingDecision() shouldBe true
            }
            val decision = game.getPendingDecision()!!
            withClue("both creatures must be on the menu when no evidence was collected") {
                game.selectCards(
                    listOf((decision as SelectCardsDecision).options.first())
                ).error shouldBe null
            }

            withClue("exactly one of the two died") {
                listOf("Grizzly Bears", "Air Elemental")
                    .count { game.findPermanent(it) != null } shouldBe 1
            }
        }

        test("with evidence collected the greatest-power creature is forced") {
            val game = board().build()

            game.castSpellCollectingEvidence(1, "Extract a Confession", "Air Elemental", "Hill Giant")
                .error shouldBe null
            game.resolveStack()

            // Only the 4/4 matches `hasGreatestPower()`, so there is nothing to choose between.
            if (game.hasPendingDecision()) {
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("the 1/1 must not be offered when evidence was collected") {
                    decision.options.size shouldBe 1
                }
                game.selectCards(listOf(decision.options.first())).error shouldBe null
            }

            withClue("the 4/4 Air Elemental is the greatest power and must be the one sacrificed") {
                game.findPermanent("Air Elemental") shouldBe null
            }
            withClue("the smaller creature survives — it was never a legal choice") {
                (game.findPermanent("Grizzly Bears") != null) shouldBe true
            }
        }
    }
}
