package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.support.answerForcedPayments
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
 * resolved by `GatedEffectExecutor.executeMayPayX`: a 0..max-affordable number chooser whose chosen
 * value is paid in generic mana and bound into `then`'s context as X. The three cases pin the gate's
 * success / decline / unaffordable-skip branches against the card's oracle rulings ("X can be zero,
 * but then [nothing happens]").
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
                val decision = game.getPendingDecision()
                withClue("MayPayX must surface a number chooser") {
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

            test("declining (X=0) creates no tokens") {
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
                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()

                game.chooseNumber(0)
                game.resolveStack()

                withClue("Declining the payment should create no tokens (X can be zero → nothing)") {
                    game.findPermanents("Soldier Token").shouldBe(emptyList())
                }
            }

            test("no payable mana pins the number chooser to 0 — no tokens") {
                // Exactly enough Plains for cycling {2}{W} (3) and none left over: the gate is
                // unaffordable, but the hand could hold a Spirit Guide, so the chooser is still
                // shown — pinned to 0.
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
                game.answerForcedPayments()

                withClue("Unaffordable MayPayX leaves nothing pending once its forced 0 is chosen") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("No mana to pay → no Soldier tokens") {
                    game.findPermanents("Soldier Token").shouldBe(emptyList())
                }
            }
        }
    }
}
