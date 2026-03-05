package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Dragon Grip.
 *
 * Card reference:
 * - Dragon Grip ({2}{R}): Enchantment — Aura
 *   "Ferocious — If you control a creature with power 4 or greater, you may cast
 *   this spell as though it had flash."
 *   "Enchant creature"
 *   "Enchanted creature gets +2/+0 and has first strike."
 */
class DragonGripScenarioTest : ScenarioTestBase() {

    init {
        context("Dragon Grip aura effects") {
            test("enchanted creature gets +2/+0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dragon Grip")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Dragon Grip", glorySeekerID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Dragon Grip should be on the battlefield") {
                    game.isOnBattlefield("Dragon Grip") shouldBe true
                }

                val clientState = game.getClientState(1)
                val creatureInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker should be 4/2 with Dragon Grip") {
                    creatureInfo shouldNotBe null
                    creatureInfo!!.power shouldBe 4
                    creatureInfo.toughness shouldBe 2
                }
            }
        }

        context("Dragon Grip conditional flash (Ferocious)") {
            test("can cast at instant speed when controlling a creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dragon Grip")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 target
                    .withCardOnBattlefield(1, "Tusked Colossodon") // 6/5 — enables ferocious
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withPriorityPlayer(1) // Player 1 has priority
                    .build()

                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Dragon Grip", glorySeekerID)
                withClue("Should be able to cast Dragon Grip at instant speed with ferocious") {
                    castResult.error shouldBe null
                }
            }

            test("cannot cast at instant speed without controlling a creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dragon Grip")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 — no ferocious
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withPriorityPlayer(1) // Player 1 has priority
                    .build()

                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Dragon Grip", glorySeekerID)
                withClue("Should not be able to cast Dragon Grip at instant speed without ferocious") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }
}
