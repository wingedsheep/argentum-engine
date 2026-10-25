package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Retriever Phoenix (STX #113) — "When this creature enters, if you cast it, learn. As long as
 * this card is in your graveyard, if you would learn, you may instead return this card to the
 * battlefield."
 *
 * The last line is a replacement effect on the Learn keyword action (CR 701.48, CR 614.1a) that
 * functions only from the graveyard (CR 113.6). The tests pin each part of that sentence:
 *  - "you may instead": yes returns the Phoenix and the learn doesn't happen; no learns as printed;
 *  - "in your graveyard": a Phoenix on the battlefield replaces nothing (the ruling: it must be in
 *    the graveyard as the learn would happen);
 *  - "if you would learn": an opponent's learn isn't replaced;
 *  - two Phoenixes: only one can replace a single learn (CR 616.1 — once one applied, the learn is
 *    gone), and declining one offers the next;
 *  - "if you cast it": a Phoenix returned this way wasn't cast, so it doesn't learn again.
 *
 * Hunt for Specimens ({1}{B} sorcery: create a Pest, learn) is the learn source.
 */
class RetrieverPhoenixScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.huntForSpecimensTurn(): ScenarioBuilder = this
        .withCardInHand(1, "Hunt for Specimens")
        .withCardInHand(1, "Grizzly Bears")   // something for a real learn to discard
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("from the graveyard, if you would learn") {
            test("yes: the Phoenix returns to the battlefield instead, and the learn doesn't happen") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .huntForSpecimensTurn()
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .build()

                game.castSpell(1, "Hunt for Specimens").error shouldBe null
                game.resolveStack()

                withClue("the learn is replaced by an optional yes/no from the Phoenix") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                withClue("Retriever Phoenix is back on the battlefield") {
                    game.isOnBattlefield("Retriever Phoenix") shouldBe true
                    game.isInGraveyard(1, "Retriever Phoenix") shouldBe false
                }
                withClue("the learn never happened, and the returned Phoenix wasn't cast so it doesn't learn") {
                    game.hasPendingDecision() shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("no: the player learns as printed and the Phoenix stays in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .huntForSpecimensTurn()
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .withCardInLibrary(1, "Hill Giant")
                    .build()

                game.castSpell(1, "Hunt for Specimens").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)

                val discard = game.getPendingDecision()
                withClue("declining the replacement leaves the learn's own discard offer") {
                    discard.shouldBeInstanceOf<SelectCardsDecision>()
                }
                val bears = (discard as SelectCardsDecision).cardInfo!!.entries
                    .first { it.value.name == "Grizzly Bears" }.key
                game.selectCards(listOf(bears))

                withClue("the discard drew a card, and the Phoenix stayed put") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Retriever Phoenix") shouldBe true
                }
            }

            test("an opponent's learn is not replaced") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .withCardInHand(2, "Hunt for Specimens")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Hunt for Specimens").error shouldBe null
                game.resolveStack()

                withClue("Player2 learns with nothing to discard or fetch — no prompt, no Phoenix offer") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Retriever Phoenix") shouldBe true
                }
            }

            test("with two Phoenixes, only one returns — declining the first offers the second") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .huntForSpecimensTurn()
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .build()

                game.castSpell(1, "Hunt for Specimens").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)

                withClue("the second Phoenix is offered after the first is declined") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                withClue("one Phoenix returned and the other is still in the graveyard") {
                    game.findAllPermanents("Retriever Phoenix").size shouldBe 1
                    game.isInGraveyard(1, "Retriever Phoenix") shouldBe true
                }
                withClue("that replacement consumed the learn — nothing more is asked") {
                    game.hasPendingDecision() shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }
        }

        context("when this creature enters, if you cast it, learn") {
            test("a cast Phoenix learns, and its own replacement does nothing from the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Retriever Phoenix")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Retriever Phoenix").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger learns as printed — a discard offer, no replacement prompt") {
                    game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                }
            }

            test("casting a Phoenix lets its learn return another Phoenix from the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Retriever Phoenix")
                    .withCardInGraveyard(1, "Retriever Phoenix")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Retriever Phoenix").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)

                withClue("both Phoenixes are on the battlefield, and nothing more is asked") {
                    game.findAllPermanents("Retriever Phoenix").size shouldBe 2
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("a Phoenix that dies with its trigger on the stack still learns, and can return itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Retriever Phoenix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Retriever Phoenix").error shouldBe null
                game.passPriority()
                game.passPriority()   // the Phoenix resolves; its enters trigger goes on the stack
                withClue("the enters trigger is waiting on the stack") {
                    game.isOnBattlefield("Retriever Phoenix") shouldBe true
                    game.state.stack.size shouldBe 1
                }

                val phoenix = game.findPermanent("Retriever Phoenix")!!
                game.castSpell(1, "Lightning Bolt", phoenix).error shouldBe null
                game.resolveStack()

                withClue("'if you cast it' reads the Phoenix as it last existed (CR 603.4), so the learn " +
                    "happens — and the Phoenix, now in the graveyard, may replace it") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                withClue("the Phoenix is back, returned by its own trigger's learn") {
                    game.isOnBattlefield("Retriever Phoenix") shouldBe true
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
