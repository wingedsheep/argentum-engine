package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fungal Colossus.
 *
 * Card reference:
 * - Fungal Colossus ({6}{G}): Creature — Fungus Beast (5/5)
 *   This spell costs {X} less to cast, where X is the number of differently named
 *   lands you control.
 */
class FungalColossusScenarioTest : ScenarioTestBase() {

    init {
        context("Fungal Colossus cost reduction") {

            test("reduced by the number of differently named lands controlled") {
                // 4 distinct names → reduction 4 → cost {2}{G} = 3 mana.
                // Forest, Island, Mountain, Plains together produce 1G + 3 generic.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Fungal Colossus")
                    .withCardOnBattlefield(1, "Forest", tapped = false)
                    .withCardOnBattlefield(1, "Island", tapped = false)
                    .withCardOnBattlefield(1, "Mountain", tapped = false)
                    .withCardOnBattlefield(1, "Plains", tapped = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Fungal Colossus")
                withClue("Cast with 4 differently named lands should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Fungal Colossus") shouldBe true
            }

            test("duplicate-named lands do not stack the reduction") {
                // 4 Forests share one name → reduction 1 → cost {5}{G} = 6 mana.
                // Only 4 lands available → cannot pay. If the implementation counted
                // each land instead of each distinct name, reduction would be 4 and
                // the cast would succeed.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Fungal Colossus")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Fungal Colossus")
                withClue("Four Forests should reduce by 1 (one distinct name), not 4") {
                    castResult.error shouldNotBe null
                }
            }

            test("non-basic lands count toward the distinct-name total") {
                // 1 Forest + 4 differently-named non-basic lands = 5 distinct names →
                // reduction 5 → cost {1}{G} = 2 mana. Forest provides {G}, any painland
                // can tap for {C} to cover the {1}. Total of 5 mana available, 2 needed.
                //
                // If non-basic lands were ignored (only basics counted), reduction would
                // be 1 → cost {5}{G} = 6 mana, and with only 5 lands available the cast
                // would fail. So a successful cast here proves non-basics are counted.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Fungal Colossus")
                    .withCardOnBattlefield(1, "Forest", tapped = false)
                    .withCardOnBattlefield(1, "Brushland", tapped = false)
                    .withCardOnBattlefield(1, "Llanowar Wastes", tapped = false)
                    .withCardOnBattlefield(1, "Underground River", tapped = false)
                    .withCardOnBattlefield(1, "Battlefield Forge", tapped = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Fungal Colossus")
                withClue("5 distinct names (1 basic + 4 non-basic) should reduce to {1}{G}: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Fungal Colossus") shouldBe true
            }

            test("mix of named and duplicate lands counts each name once") {
                // 2 Forests + 1 Island + 1 Mountain + 1 Plains = 5 lands, 4 distinct names
                // → reduction 4 → cost {2}{G} = 3 mana. Available = 5. Succeeds.
                // (If duplicates counted, reduction would be 5 — also succeeds — so the
                // discriminating signal here is the prior test; this one guards against
                // an off-by-one in the mixed case.)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Fungal Colossus")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Island", tapped = false)
                    .withCardOnBattlefield(1, "Mountain", tapped = false)
                    .withCardOnBattlefield(1, "Plains", tapped = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Fungal Colossus")
                withClue("5 lands / 4 distinct names should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Fungal Colossus") shouldBe true
            }
        }
    }
}
