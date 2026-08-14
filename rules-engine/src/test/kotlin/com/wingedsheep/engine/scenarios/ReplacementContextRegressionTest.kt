package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A replacement effect's `restrictions` — and the context its outcome executes under — must
 * be resolved against the player who is actually drawing, not against whoever announced the
 * draw. `ReplacementEffectProcessor` prefers the caller-supplied `EffectContext`, which for a
 * spell is the spell's own context.
 *
 * Invisible to the existing suite because every other draw test has the replacement's
 * controller resolving the draw themselves, so the two contexts coincide.
 */
class ReplacementContextRegressionTest : ScenarioTestBase() {

    init {

        // =====================================================================
        // A `restrictions` list must be evaluated against the drawing player.
        //
        // ModifyDrawAmount's KDoc: "Each entry in restrictions is a Condition evaluated
        // against the drawing player as the controller context". Conditions.CardsInHandAtMost
        // is Compare(Count(Player.You, HAND), LTE, n), and Player.You resolves against
        // EffectContext.controllerId — so the context handed to the processor decides
        // whose hand is counted.
        // =====================================================================

        test("Quantum Riddler's hand-size restriction is checked against the drawing player, not the caster") {
            // Player 1 controls Quantum Riddler and holds 5 cards — its
            // CardsInHandAtMost(1) restriction is FALSE for player 1, so the +1 must not apply.
            // Player 2 casts Inspiration ("Target player draws two cards") at player 1 and is
            // left holding 0 cards, so the restriction is TRUE for player 2.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardsInHand(1, "Grizzly Bears", 5)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInHand(2, "Inspiration")
                .withLandsOnBattlefield(2, "Island", 4)
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(2, "Inspiration", 1)
            cast.error shouldBe null
            game.resolveStack()

            withClue(
                "Player 1 holds 5 cards, so Quantum Riddler's CardsInHandAtMost(1) is false and " +
                    "Inspiration draws exactly 2. Drawing 3 means the restriction was evaluated " +
                    "against Inspiration's controller (player 2, who has an empty hand)."
            ) {
                game.handSize(1) shouldBe 7
            }
        }

        test("Quantum Riddler's restriction still applies when the drawing player qualifies and the caster does not") {
            // The mirror of the case above: the restriction is TRUE for the drawing player
            // (player 1 holds nothing) and FALSE for the caster (player 2 keeps 5 cards after
            // casting), so the +1 must still apply.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Quantum Riddler")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInHand(2, "Inspiration")
                .withCardsInHand(2, "Grizzly Bears", 5)
                .withLandsOnBattlefield(2, "Island", 4)
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.handSize(1) shouldBe 0

            val cast = game.castSpellTargetingPlayer(2, "Inspiration", 1)
            cast.error shouldBe null
            game.resolveStack()

            withClue(
                "Player 1 holds 0 cards, so Quantum Riddler adds +1 and Inspiration draws 3. " +
                    "Drawing only 2 means the restriction was evaluated against player 2, " +
                    "who still holds 5 cards."
            ) {
                game.handSize(1) shouldBe 3
            }
        }

        test("Phial of Galadriel does not fire — and never draws for the caster — on an opponent's draw spell") {
            // The same defect on the Replaced branch rather than the Modified branch, and it
            // goes one step further: the replacement effect is also *executed* with the
            // caller's context, so the opponent performs the replacement draw.
            //
            // Player 1 controls Phial of Galadriel ("If you would draw a card while you have
            // no cards in hand, draw two cards instead") and holds 3 cards, so the replacement
            // must not fire at all. Player 2 casts Inspiration at player 1 holding nothing.
            // Player 2 gets a library too, otherwise a mis-attributed draw is silently
            // swallowed by their empty library and the defect hides.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Phial of Galadriel")
                .withCardsInHand(1, "Grizzly Bears", 3)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInHand(2, "Inspiration")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Island", 4)
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(2, "Inspiration", 1)
            cast.error shouldBe null
            game.resolveStack()

            withClue(
                "Inspiration targets player 1, so player 2 draws nothing. Drawing 2 means " +
                    "Phial of Galadriel fired (its EmptyHand restriction read player 2's hand) " +
                    "and its DrawCardsEffect(2) then resolved against player 2, the context's " +
                    "controller, instead of the drawing player."
            ) {
                game.handSize(2) shouldBe 0
            }
            withClue(
                "Player 1's hand is not empty, so Phial does not replace either draw and " +
                    "Inspiration draws exactly 2 for player 1."
            ) {
                game.handSize(1) shouldBe 5
            }
        }
    }
}
