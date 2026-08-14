package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Macabre Reconstruction (MKM #93) — {3}{B} sorcery, "costs {2} less to cast if a creature card was
 * put into your graveyard from anywhere this turn", returns up to two target creature cards from
 * your graveyard to your hand.
 *
 * Covers the new `TurnTracker.CREATURE_CARDS_PUT_INTO_GRAVEYARD` end to end. The point of the
 * tracker is that it is *turn history*, not a graveyard scan, so the tests pin the mana actually
 * required rather than reading the component: three lands can't cast it cold even with creature
 * cards sitting in the yard from before, three lands *can* cast it once a creature has died this
 * turn, and the discount is gone again next turn.
 *
 * Killing your own Grizzly Bears with a Lightning Bolt is the outlet — it routes the creature card
 * through `ZoneTransitionService` the same way any death does, and it leaves two Swamps untapped,
 * exactly the reduced {1}{B}.
 */
class MacabreReconstructionScenarioTest : ScenarioTestBase() {

    /**
     * Shared board: the Reconstruction and a Bolt in hand, a 2/2 of your own to point the Bolt at,
     * two creature cards already in the graveyard (legal targets — and proof that a pre-existing
     * graveyard doesn't discount anything), and one Mountain plus two Swamps.
     *
     * Three lands is the whole test: {3}{B} needs four mana, the reduced {1}{B} needs two, and the
     * Mountain pays for the Bolt.
     */
    private fun board(swamps: Int = 2): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Macabre Reconstruction")
            .withCardInHand(1, "Lightning Bolt")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInGraveyard(1, "Craw Wurm")
            .withCardInGraveyard(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Swamp", swamps)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) {
            builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        }
        return builder
    }

    /** Bolt your own Bears so a creature card lands in your graveyard this turn. */
    private fun boltYourOwnBears(game: TestGame) {
        val bears = game.findPermanent("Grizzly Bears") ?: error("no Grizzly Bears on the battlefield")
        game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
        game.resolveStack()
        withClue("the Bears must actually have died") {
            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }

    /** Cast the Reconstruction naming [targetNames] in your own graveyard. */
    private fun castTargeting(game: TestGame, vararg targetNames: String) =
        game.execute(
            CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Macabre Reconstruction").first(),
                targets = targetNames.map { name ->
                    val id = game.state.getGraveyard(game.player1Id).first { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == name
                    }
                    ChosenTarget.Card(id, game.player1Id, Zone.GRAVEYARD)
                },
            )
        )

    init {
        test("costs its full {3}{B} when no creature card hit your graveyard this turn") {
            val game = board().build()

            withClue("two creature cards already in the yard are not the trigger — they arrived earlier") {
                castTargeting(game, "Craw Wurm").error shouldNotBe null
            }
        }

        test("a creature dying this turn discounts it to {1}{B} and it returns two cards") {
            val game = board().build()

            boltYourOwnBears(game)

            withClue("the {2} reduction must bring {3}{B} within reach of two Swamps") {
                castTargeting(game, "Craw Wurm", "Hill Giant").error shouldBe null
            }
            game.resolveStack()

            withClue("both targets go back to hand") {
                game.isInHand(1, "Craw Wurm") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
            }
        }

        test("'up to two' accepts a single target") {
            val game = board().build()

            boltYourOwnBears(game)

            castTargeting(game, "Hill Giant").error shouldBe null
            game.resolveStack()

            withClue("only the named card comes back") {
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Craw Wurm") shouldBe true
            }
        }

        test("one death discounts every cast that turn, and survives the card leaving the yard") {
            // Four Swamps is exactly two discounted casts. An undiscounted second cast would need
            // four mana with only two Swamps left, so this fails loudly if the discount were
            // consumed by the first cast or undone by pulling the dead creature back out.
            val game = board(swamps = 4).withCardInHand(1, "Macabre Reconstruction").build()

            boltYourOwnBears(game)

            withClue("first cast: discounted, and it takes the Bears back out of the graveyard") {
                castTargeting(game, "Grizzly Bears").error shouldBe null
            }
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false

            withClue("second cast: the same death still discounts it") {
                castTargeting(game, "Hill Giant").error shouldBe null
            }
            game.resolveStack()
            game.isInHand(1, "Hill Giant") shouldBe true
        }

        test("the discount is turn-scoped — it's gone next turn") {
            val game = board().build()

            boltYourOwnBears(game)

            // Round the table back to player 1's next main phase. Each stop has to be a distinct
            // (phase, step) or passUntilPhase returns immediately without advancing.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 2's upkeep
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // player 2's main
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 1's next upkeep
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // player 1's next main
            withClue("we must actually be back on player 1's turn") {
                game.state.activePlayerId shouldBe game.player1Id
            }

            withClue("a creature that died last turn must not discount this spell") {
                castTargeting(game, "Craw Wurm").error shouldNotBe null
            }
        }
    }
}
