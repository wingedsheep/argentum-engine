package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Override (MRD #45) — {2}{U} Instant.
 *
 *   Counter target spell unless its controller pays {1} for each artifact you control.
 *
 * The trap in this wording is the pronoun: "you" is *Override's* controller, so the tax is set by
 * the counterspeller's artifacts while the bill is paid by the spell's controller. Player 1 always
 * casts Override here and Player 2 always casts Grizzly Bears, so the tests can move the artifacts
 * from one side of the table to the other and read the tax:
 *
 *  - two artifacts on the caster's side → a {2} offer, declined → countered;
 *  - the same {2}, paid → Grizzly Bears resolves;
 *  - no artifacts anywhere → the amount is 0, so no offer is made at all and the spell resolves
 *    for free (CR: "unless its controller pays {0}" is not a counter);
 *  - two artifacts on the *spell controller's* side → still 0, proving the count is not read off
 *    the player being taxed.
 */
class OverrideScenarioTest : ScenarioTestBase() {

    init {
        context("Override") {

            /**
             * @param myArtifacts artifacts controlled by Player 1, who casts Override
             * @param theirArtifacts artifacts controlled by Player 2, who casts Grizzly Bears
             */
            fun board(myArtifacts: Int = 0, theirArtifacts: Int = 0) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Override")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(myArtifacts) { withCardOnBattlefield(1, "Bonesplitter") } }
                .apply { repeat(theirArtifacts) { withCardOnBattlefield(2, "Bonesplitter") } }
                .build()

            fun TestGame.castBearsThenOverride() {
                castSpell(2, "Grizzly Bears").error shouldBe null
                passPriority()
                castSpellTargetingStackSpell(1, "Override", "Grizzly Bears").error shouldBe null
                resolveStack()
            }

            test("taxes the spell's controller {1} for each artifact Override's controller has") {
                val game = board(myArtifacts = 2)
                game.castBearsThenOverride()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<YesNoDecision>()
                withClue("The bill goes to the spell's controller, not Override's") {
                    decision.playerId shouldBe game.player2Id
                }
                withClue("Two artifacts on Player 1's side is exactly {2} — not {1}, not {3}") {
                    decision.prompt shouldContain "{2}"
                }

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("Declining the tax counters Grizzly Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("paying the tax saves the spell") {
                val game = board(myArtifacts = 2)
                game.castBearsThenOverride()

                game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<YesNoDecision>()

                // Grizzly Bears ate two of the four Forests; the other two are exactly the {2}.
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Player 2 paid, so the spell is not countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("with no artifacts the tax is {0}, so nothing is countered and nothing is asked") {
                val game = board()
                game.castBearsThenOverride()

                withClue("A {0} tax makes no offer — there is nothing to decline") {
                    game.getPendingDecision().shouldBeNull()
                }
                game.resolveStack()

                withClue("The spell resolves for free rather than being countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("the artifacts counted are Override's controller's, not the spell controller's") {
                val game = board(theirArtifacts = 2)
                game.castBearsThenOverride()

                withClue("Player 2's own artifacts must not tax Player 2 — 'you' is Player 1") {
                    game.getPendingDecision().shouldBeNull()
                }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
