package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Settle the Wreckage (XLN #34, reprinted as HOB #26) — {2}{W}{W} Instant.
 *
 * Oracle: "Exile all attacking creatures target player controls. That player may search their
 * library for that many basic land cards, put those cards onto the battlefield tapped, then
 * shuffle."
 *
 * The three things that can silently go wrong: the wipe is a *group* scoped to the target player
 * (not a target, and not "all attackers"), the fetch is done by the **target player** and not the
 * caster, and its cap is "that many" — the number of creatures exiled.
 */
class SettleTheWreckageScenarioTest : ScenarioTestBase() {

    init {
        context("Settle the Wreckage") {

            test("exiles only the target player's attackers and lets them fetch that many basics") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Settle the Wreckage")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    // Four basics in the library, but only two creatures will be exiled.
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Savannah Lions stays home, so it must survive the wipe.
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)
                ).error shouldBe null

                // The attacking player holds priority first; pass it to the defender.
                game.passPriority()
                game.castSpellTargetingPlayer(2, "Settle the Wreckage", 1).error shouldBe null
                game.resolveStack()

                withClue("both attackers are exiled") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.findPermanent("Hill Giant") shouldBe null
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Hill Giant") shouldBe true
                }
                withClue("the non-attacking creature is untouched") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }

                withClue("player 1 — the *target*, not the caster — is asked whether to search") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                val search = game.getPendingDecision() as SelectCardsDecision
                withClue("'that many' caps the search at the two creatures exiled, not at the four basics available") {
                    search.playerId shouldBe game.player1Id
                    search.options.size shouldBe 4
                    search.maxSelections shouldBe 2
                    search.minSelections shouldBe 0
                }

                game.selectCards(search.options.take(2)).error shouldBe null
                game.resolveStack()

                val fetched = game.findAllPermanents("Forest")
                withClue("both basics entered tapped under the target player's control") {
                    fetched.size shouldBe 2
                    fetched.all { game.state.getBattlefield(game.player1Id).contains(it) } shouldBe true
                    fetched.all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
            }

            test("declining the search leaves the library alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Settle the Wreckage")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                // The attacking player holds priority first; pass it to the defender.
                game.passPriority()
                game.castSpellTargetingPlayer(2, "Settle the Wreckage", 1).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                withClue("the declined search fetches nothing") {
                    game.findPermanent("Forest") shouldBe null
                    game.librarySize(1) shouldBe 1
                }
            }
        }
    }
}
