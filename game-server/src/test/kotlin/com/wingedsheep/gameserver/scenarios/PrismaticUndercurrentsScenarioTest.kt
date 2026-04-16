package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PrismaticUndercurrentsScenarioTest : ScenarioTestBase() {

    init {
        context("Prismatic Undercurrents") {

            test("ETB searches for up to X basic lands where X equals colors among permanents you control") {
                // Player 1 controls:
                //   - Forest x4 (colorless, provides mana)
                //   - Lavaleaper (red creature) — contributes red
                //   - Silvergill Peddler (blue creature) — contributes blue
                // When Prismatic Undercurrents enters, it contributes green.
                // Total colors = green + red + blue = 3 → X = 3.
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Prismatic Undercurrents")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Lavaleaper")
                    .withCardOnBattlefield(1, "Silvergill Peddler")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Prismatic Undercurrents ({3}{G} — 4 forests cover the cost)
                game.castSpell(1, "Prismatic Undercurrents")

                // The spell resolves, enchantment enters — process ETB trigger
                game.resolveStack()
                game.resolveStack()

                val handSizeAfterCast = game.handSize(1)

                // Should have a library search decision pending
                val decision = game.getPendingDecision()
                withClue("ETB should generate a library search decision") {
                    decision shouldNotBe null
                }
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                // X = 3 colors (green from enchantment, red from Lavaleaper, blue from Silvergill Peddler)
                withClue("Max selections should equal number of colors among your permanents (3)") {
                    decision.maxSelections shouldBe 3
                }

                withClue("Options should include basic lands from library") {
                    decision.options.size shouldBeGreaterThanOrEqual 1
                }

                // Select 2 basic lands
                val selectedLands = decision.options.take(2)
                game.selectCards(selectedLands)

                withClue("Hand should grow by 2 from the search") {
                    game.handSize(1) shouldBe handSizeAfterCast + 2
                }

                withClue("Library should have 2 fewer cards after shuffle") {
                    game.librarySize(1) shouldBe 2
                }
            }

            test("ETB with only the enchantment itself as colored permanent gives X=1") {
                // Only Prismatic Undercurrents (green) is on the battlefield after it enters.
                // No other colored permanents → X = 1.
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Prismatic Undercurrents")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Prismatic Undercurrents")
                game.resolveStack()
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("Should have a library search decision") {
                    decision shouldNotBe null
                }
                decision.shouldBeInstanceOf<SelectCardsDecision>()

                withClue("Max selections should be 1 (only green from the enchantment itself)") {
                    decision.maxSelections shouldBe 1
                }
            }

            test("grants an additional land drop while on battlefield") {
                // Put Prismatic Undercurrents already on battlefield so no ETB trigger fires
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Prismatic Undercurrents")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Plains")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Play first land (normal land drop)
                val firstLand = game.findCardsInHand(1, "Forest").first()
                val firstResult = game.execute(PlayLand(game.player1Id, firstLand))
                withClue("First land should be playable") {
                    firstResult.error shouldBe null
                }

                // Play second land (granted by Prismatic Undercurrents)
                val secondLand = game.findCardsInHand(1, "Plains").first()
                val secondResult = game.execute(PlayLand(game.player1Id, secondLand))
                withClue("Second land should be playable due to Prismatic Undercurrents") {
                    secondResult.error shouldBe null
                }
            }

            test("two Prismatic Undercurrents grant two extra land drops (cumulative)") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Prismatic Undercurrents")
                    .withCardOnBattlefield(1, "Prismatic Undercurrents")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Should be able to play 3 lands (1 base + 2 from two enchantments)
                for (i in 1..3) {
                    val land = game.findCardsInHand(1, "Forest").first()
                    val result = game.execute(PlayLand(game.player1Id, land))
                    withClue("Land drop $i/3 should succeed with two Prismatic Undercurrents") {
                        result.error shouldBe null
                    }
                }
            }
        }
    }
}
