package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Tend the Sprigs.
 *
 * Tend the Sprigs ({2}{G} Sorcery):
 *   "Search your library for a basic land card, put it onto the battlefield tapped,
 *    then shuffle. Then if you control seven or more lands and/or Treefolk, create a
 *    3/4 green Treefolk creature token with reach."
 *
 * The conditional second clause must only fire when the controller actually controls
 * seven or more lands and/or Treefolk.
 */
class TendTheSprigsScenarioTest : ScenarioTestBase() {

    /** Search-to-battlefield pauses for a card selection; pick the first basic land found. */
    private fun TestGame.fetchFirstBasicLand() {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        selectCards(decision.options.take(1))
    }

    init {
        context("Tend the Sprigs - conditional Treefolk token") {

            test("does NOT create a Treefolk token with fewer than 7 lands/Treefolk") {
                // 3 Forests pay {2}{G}; fetching 1 brings the total to 4 lands — below 7.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                withClue("Tend the Sprigs should cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("Only 4 lands are controlled — no token should be created") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("does NOT count non-land, non-Treefolk creatures toward the 7") {
                // 3 Forests + 6 Grizzly Bears; fetch 1 land => 4 lands, 6 non-Treefolk creatures.
                // Lands/Treefolk = 4 (< 7). If the filter is ignored, the 10 permanents trip it.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("Only 4 lands controlled (6 Bears are not lands/Treefolk) — no token") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("does NOT create a token at exactly 6 lands (strict 7+ threshold)") {
                // 5 Forests pay {2}{G}; fetch 1 brings the total to 6 lands — still below 7.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("6 lands controlled (< 7) — no token should be created") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("does NOT count the opponent's lands toward your 7") {
                // Caster controls 3 Forests; fetch 1 => 4 lands. Opponent has 10 Forests.
                // "lands you control" must ignore the opponent's lands.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(2, "Forest", 10)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("Caster controls 4 lands; opponent's 10 lands must not count — no token") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("declining the search does not over-count at 6 lands") {
                // 6 Forests, a basic land available to fetch, but the player declines.
                // Total stays at 6 lands — no token.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                game.skipSelection()
                game.resolveStack()

                withClue("Search declined; only 6 lands controlled — no token") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("resolves the synchronous no-find path correctly at 6 lands") {
                // Library has no basic land, so the search resolves without a selection pause.
                // This exercises the synchronous (non-resume) path into the ConditionalEffect.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                // A SelectCardsDecision with no valid options may still surface; skip if present.
                if (game.hasPendingDecision()) game.skipSelection()
                game.resolveStack()

                withClue("No land fetched; only 6 lands controlled — no token") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("does NOT count lands in the graveyard toward your 7") {
                // 3 Forests on the battlefield + fetch 1 => 4 lands in play.
                // 5 more Forests sit in the graveyard. Only battlefield lands count.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                cast.error shouldBe null
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("4 lands in play; the 5 graveyard lands must not count — no token") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 0
                }
            }

            test("creates a Treefolk token with 7 or more lands/Treefolk") {
                // 6 Forests pay {2}{G}; fetching 1 brings the total to 7 lands.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tend the Sprigs")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Tend the Sprigs")
                withClue("Tend the Sprigs should cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                game.fetchFirstBasicLand()
                game.resolveStack()

                withClue("7 lands are controlled — a Treefolk token should be created") {
                    game.findAllPermanents("Treefolk Token").size shouldBe 1
                }
            }
        }
    }
}
