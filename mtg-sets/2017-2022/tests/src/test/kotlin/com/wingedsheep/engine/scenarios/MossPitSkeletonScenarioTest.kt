package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Moss-Pit Skeleton (ZNR #228).
 *
 *   Kicker {3}
 *   If this creature was kicked, it enters with three +1/+1 counters on it.
 *   Whenever one or more +1/+1 counters are put on a creature you control, if this card is in
 *   your graveyard, you may put this card on top of your library.
 *
 * Pins the graveyard-only trigger: it fires off counters on your creature, the "may" can be
 * declined, an opponent's creature doesn't count, and counters on two creatures at once trigger
 * it twice — the second instance doing nothing once the card has left the graveyard.
 */
class MossPitSkeletonScenarioTest : ScenarioTestBase() {

    init {
        fun setup() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Moss-Pit Skeleton")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withCardInHand(1, "Battlegrowth")
            .withCardInHand(1, "Travel Preparations")
            .withLandsOnBattlefield(1, "Forest", 3)
            .apply { repeat(3) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Moss-Pit Skeleton") {

            test("a +1/+1 counter on your creature lets you put it on top of your library") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Battlegrowth", bears).error shouldBe null
                game.resolveStack()

                withClue("the graveyard trigger offers the move") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                game.isInGraveyard(1, "Moss-Pit Skeleton") shouldBe false
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Moss-Pit Skeleton").single()
            }

            test("declining leaves it in the graveyard") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Battlegrowth", bears).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Moss-Pit Skeleton") shouldBe true
            }

            test("a counter on an opponent's creature doesn't trigger it") {
                val game = setup()
                val theirLions = game.findPermanent("Savannah Lions")!!
                game.castSpell(1, "Battlegrowth", theirLions).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Moss-Pit Skeleton") shouldBe true
            }

            test("counters on two creatures trigger it twice; the second does nothing after the move") {
                val game = setup()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val prep = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Travel Preparations"
                }
                game.execute(
                    CastSpell(
                        game.player1Id,
                        prep,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null
                // Resolve the sorcery only; both triggers then go on the (otherwise empty) stack.
                var passes = 0
                while (prep in game.state.stack && passes++ < 4) game.passPriority().error shouldBe null

                withClue("one trigger per creature that got counters") {
                    game.state.stack.size shouldBe 2
                }

                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the second instance's intervening-if fails, so there's no second prompt") {
                    game.hasPendingDecision() shouldBe false
                }
                game.state.stack.size shouldBe 0
                game.findCardsInLibrary(1, "Moss-Pit Skeleton").size shouldBe 1
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Moss-Pit Skeleton").single()
            }
        }
    }
}
