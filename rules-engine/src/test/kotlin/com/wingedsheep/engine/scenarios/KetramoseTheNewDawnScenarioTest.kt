package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ketramose, the New Dawn (DFT #209) — {1}{W}{B} Legendary Creature — God, 4/4.
 *
 *   Menace, lifelink, indestructible
 *   Ketramose can't attack or block unless there are seven or more cards in exile.
 *   Whenever one or more cards are put into exile from graveyards and/or the battlefield during
 *   your turn, you draw a card and lose 1 life.
 *
 * Two things are worth proving beyond the happy path:
 *  - the exile count is **global** (Scryfall ruling 2025-02-07 — "regardless of who owns them"),
 *    so seven cards in the *opponent's* exile enable the attack just as well as your own; and
 *  - the draw trigger is a CR 603.2c **batch** — Rest in Peace sweeping several graveyard cards
 *    into exile at once must draw one card and cost one life, not one per card.
 */
class KetramoseTheNewDawnScenarioTest : ScenarioTestBase() {

    init {
        context("Ketramose, the New Dawn — attack/block gate on seven cards in exile") {

            test("cannot attack with only six cards in exile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .apply { repeat(6) { withCardInExile(1, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("six cards in exile is one short — the attack must be rejected") {
                    game.declareAttackers(mapOf("Ketramose, the New Dawn" to 2)).error shouldNotBe null
                }
            }

            test("can attack with seven cards in exile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .apply { repeat(7) { withCardInExile(1, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("seven cards in exile satisfies the restriction") {
                    game.declareAttackers(mapOf("Ketramose, the New Dawn" to 2)).error shouldBe null
                }
            }

            test("the opponent's exile counts too — the tally is global") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .apply { repeat(7) { withCardInExile(2, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("cards in Bob's exile count toward Ketramose's seven (2025-02-07 ruling)") {
                    game.declareAttackers(mapOf("Ketramose, the New Dawn" to 2)).error shouldBe null
                }
            }

            test("cannot block with only six cards in exile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(2, "Ketramose, the New Dawn", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .apply { repeat(6) { withCardInExile(1, "Hill Giant") } }
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("six cards in exile — the block must be rejected") {
                    game.declareBlockers(
                        mapOf("Ketramose, the New Dawn" to listOf("Grizzly Bears"))
                    ).error shouldNotBe null
                }
            }

            test("can block with seven cards in exile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(2, "Ketramose, the New Dawn", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .apply { repeat(7) { withCardInExile(1, "Hill Giant") } }
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("seven cards in exile satisfies the restriction") {
                    game.declareBlockers(
                        mapOf("Ketramose, the New Dawn" to listOf("Grizzly Bears"))
                    ).error shouldBe null
                }
            }
        }

        context("Ketramose, the New Dawn — exile batch trigger") {

            test("exiling a card from a graveyard during your turn draws a card and loses 1 life") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Coffin Purge")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpellTargetingGraveyardCard(1, "Coffin Purge", 2, "Grizzly Bears")
                    .error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Ketramose sees the graveyard→exile move and drains one life") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 1
                }
                withClue("the drawn card comes off the library") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("exiling a permanent from the battlefield also fires the trigger") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(1, "Wander Off")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Wander Off", bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("battlefield→exile is one of the watched source zones") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 1
                    game.handSize(1) shouldBe 1
                }
            }

            test("a multi-card exile fires the trigger only once (CR 603.2c batching)") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Rest in Peace")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpell(1, "Rest in Peace").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("four cards left graveyards in one event — that is one trigger, not four") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 1
                    game.handSize(1) shouldBe 1
                }
            }

            test("does not trigger during the opponent's turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ketramose, the New Dawn", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withCardInHand(2, "Coffin Purge")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpellTargetingGraveyardCard(2, "Coffin Purge", 1, "Grizzly Bears")
                    .error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger is gated on 'during your turn'") {
                    game.getLifeTotal(1) shouldBe lifeBefore
                    game.handSize(1) shouldBe 0
                }
            }
        }
    }
}
