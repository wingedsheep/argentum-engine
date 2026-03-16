package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Curator's Ward.
 *
 * Card reference:
 * - Curator's Ward ({2}{U}): Enchantment — Aura
 *   "Enchant permanent"
 *   "Enchanted permanent has hexproof."
 *   "When enchanted permanent leaves the battlefield, if it was historic, draw two cards."
 */
class CuratorsWardScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Curator's Ward grants hexproof") {

            test("enchanted creature has hexproof") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Steel Leaf Champion")
                    .withCardInHand(1, "Curator's Ward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Steel Leaf Champion")!!

                game.castSpell(1, "Curator's Ward", creatureId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Enchanted creature should have hexproof") {
                    projected.hasKeyword(creatureId, Keyword.HEXPROOF) shouldBe true
                }
            }
        }

        context("Curator's Ward draw trigger") {

            test("draws two cards when enchanted legendary creature is exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lyra Dawnbringer") // Legendary creature
                    .withCardInHand(1, "Curator's Ward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Blessed Light") // {4}{W} exile target creature or enchantment
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Lyra Dawnbringer")!!

                // Cast Curator's Ward on Lyra
                game.castSpell(1, "Curator's Ward", creatureId)
                game.resolveStack()

                withClue("Curator's Ward should be on the battlefield") {
                    game.isOnBattlefield("Curator's Ward") shouldBe true
                }

                // Player 1 exiles their own Lyra with Blessed Light (hexproof only prevents opponent targeting)
                val handSizeBefore = game.handSize(1)
                game.castSpell(1, "Blessed Light", creatureId)
                game.resolveStack()

                withClue("Lyra should no longer be on the battlefield") {
                    game.isOnBattlefield("Lyra Dawnbringer") shouldBe false
                }
                // Blessed Light cast: -1. Curator's Ward trigger: +2 drawn. Net: +1
                withClue("Player should have drawn 2 cards from Curator's Ward trigger") {
                    game.handSize(1) shouldBe handSizeBefore - 1 + 2
                }
            }

            test("does NOT draw cards when enchanted non-historic creature leaves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Steel Leaf Champion") // Not historic
                    .withCardInHand(1, "Curator's Ward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Blessed Light")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Steel Leaf Champion")!!

                // Cast Curator's Ward on Steel Leaf Champion
                game.castSpell(1, "Curator's Ward", creatureId)
                game.resolveStack()

                // Exile Steel Leaf Champion
                val handSizeBefore = game.handSize(1)
                game.castSpell(1, "Blessed Light", creatureId)
                game.resolveStack()

                withClue("Steel Leaf Champion should no longer be on the battlefield") {
                    game.isOnBattlefield("Steel Leaf Champion") shouldBe false
                }
                // Blessed Light cast: -1. No draw (not historic). Net: -1
                withClue("Player should NOT have drawn cards (not historic)") {
                    game.handSize(1) shouldBe handSizeBefore - 1
                }
            }

            test("draws two cards when enchanted artifact leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gilded Lotus") // Artifact (historic)
                    .withCardInHand(1, "Curator's Ward")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Invoke the Divine") // {2}{W} destroy artifact or enchantment
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifactId = game.findPermanent("Gilded Lotus")!!

                // Cast Curator's Ward on Gilded Lotus
                game.castSpell(1, "Curator's Ward", artifactId)
                game.resolveStack()

                // Destroy Gilded Lotus
                val handSizeBefore = game.handSize(1)
                game.castSpell(1, "Invoke the Divine", artifactId)
                game.resolveStack()

                withClue("Gilded Lotus should no longer be on the battlefield") {
                    game.isOnBattlefield("Gilded Lotus") shouldBe false
                }
                // Invoke cast: -1. Curator's Ward trigger: +2 drawn. Net: +1
                withClue("Player should have drawn 2 cards (artifact is historic)") {
                    game.handSize(1) shouldBe handSizeBefore - 1 + 2
                }
            }
        }
    }
}
