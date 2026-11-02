package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for the `Gate.MayPayX` gate kind — the lowered `Effects.MayPayX` wrapper
 * (gated-effect-migration handoff #5). Exercised through Decree of Justice (SCG):
 *
 * "Cycling {2}{W}
 *  When you cycle Decree of Justice, you may pay {X}. If you do, create X 1/1 white
 *  Soldier creature tokens."
 *
 * The trigger lowers to `GatedEffect(gate = Gate.MayPayX, then = CreateTokenEffect(X 1/1 Soldiers))`,
 * resolved by `GatedEffectExecutor.executeMayPayX`: a "Pay {X}?" yes/no, then on yes a
 * 0..max-affordable number chooser whose chosen value is paid in generic mana and bound into
 * `then`'s context as X. Declining and paying {0} are separate answers (X = 0 is a payment); the
 * cases pin pay / pay-zero / decline / no-mana against the card's ruling that X can be zero.
 */
class DecreeOfJusticeMayPayXTest : ScenarioTestBase() {

    init {
        context("Decree of Justice — \"you may pay {X}\" cycling trigger (Gate.MayPayX)") {

            test("paying X=2 creates two 1/1 Soldier tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.cycleCard(1, "Decree of Justice")
                withClue("Cycle should succeed: ${cycle.error}") { cycle.error shouldBe null }

                // The "when you cycle" trigger resolves before the cycling draw and pauses on the
                // MayPayX number chooser. Cycling tapped 3 of 6 Plains, leaving 3 payable.
                game.resolveStack()
                withClue("MayPayX first asks whether to pay at all") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                withClue("Paying surfaces a number chooser") {
                    decision.shouldBeInstanceOf<ChooseNumberDecision>()
                }
                withClue("Max X = untapped Plains after cycling = 3") {
                    (decision as ChooseNumberDecision).maxValue shouldBe 3
                }

                game.chooseNumber(2)
                game.resolveStack()

                withClue("Paying X=2 should create exactly two Soldier tokens") {
                    game.findPermanents("Soldier Token").size shouldBe 2
                }
            }

            test("paying X=0 creates no tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Decree of Justice")
                game.resolveStack()
                game.answerYesNo(true)
                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()

                game.chooseNumber(0)
                game.resolveStack()

                withClue("Paying {0} creates zero tokens") {
                    game.findPermanents("Soldier Token").shouldBe(emptyList())
                }
            }

            test("declining creates no tokens and asks for no number") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Decree of Justice")
                game.resolveStack()
                game.answerYesNo(false)

                withClue("Declining ends the gate without a number chooser") {
                    (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
                }
                game.resolveStack()
                withClue("Declining the payment creates no tokens") {
                    game.findPermanents("Soldier Token").shouldBe(emptyList())
                }
            }

            test("with no payable mana, paying is still offered and pays X=0 without a number chooser") {
                // Exactly enough Plains for cycling {2}{W} (3) and none left over: {0} is the only
                // payment, so "yes" pays it outright.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Decree of Justice")
                game.resolveStack()
                withClue("Paying {0} is still a choice") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                withClue("With nothing payable but 0, no number chooser follows") {
                    (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
                }
                game.resolveStack()
                withClue("No mana to pay → no Soldier tokens") {
                    game.findPermanents("Soldier Token").shouldBe(emptyList())
                }
            }
        }
    }
}
