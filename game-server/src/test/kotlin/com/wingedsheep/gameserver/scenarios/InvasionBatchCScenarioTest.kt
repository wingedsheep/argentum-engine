package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Invasion "batch C" cards:
 *   - Essence Leak  ({U} Aura) — conditional granted upkeep pay-or-sacrifice ("pay its mana cost")
 *   - Juntu Stakes  ({2} Artifact) — creatures with power 1 or less don't untap
 *   - Kavu Lair     ({2}{G} Enchantment) — power-4+ creature enters → its controller draws
 *
 * Exercises the new engine pieces: PayCost.OwnManaCost and the EnchantedPermanentMatches condition.
 */
class InvasionBatchCScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, entityId: EntityId): Boolean =
        game.state.getEntity(entityId)?.get<TappedComponent>() != null

    init {
        context("Juntu Stakes — power 1 or less don't untap") {
            test("a power-1 creature stays tapped through its untap step; a power-2 creature untaps") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Juntu Stakes")
                    .withCardOnBattlefield(1, "Storm Crow", tapped = true)    // 1/2 — should NOT untap
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true) // 2/2 — should untap
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                val stormCrow = game.findPermanent("Storm Crow")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Advance from P2's turn into P1's untap + upkeep so P1's permanents untap.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("Storm Crow (power 1) does not untap") {
                    isTapped(game, stormCrow) shouldBe true
                }
                withClue("Grizzly Bears (power 2) untaps normally") {
                    isTapped(game, bears) shouldBe false
                }
            }
        }

        context("Kavu Lair — power 4+ enters → controller draws") {
            test("casting a 5/5 creature draws a card for its controller") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kavu Lair")
                    .withCardInHand(1, "Hulking Cyclops")          // 5/5, {3}{R}{R}
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val castResult = game.castSpell(1, "Hulking Cyclops")
                withClue("Hulking Cyclops casts: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hulking Cyclops resolved onto the battlefield") {
                    game.isOnBattlefield("Hulking Cyclops") shouldBe true
                }
                // Spent the card from hand (-1) but drew one from Kavu Lair (+1): net same count.
                withClue("Kavu Lair drew P1 a card for the power-5 creature entering") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("a power-3 creature entering does not draw") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kavu Lair")
                    .withCardInHand(1, "Hill Giant")               // 3/3, {3}{R}
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val castResult = game.castSpell(1, "Hill Giant")
                withClue("Hill Giant casts: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant resolved onto the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                // Spent the card (-1), no draw: hand shrinks by one.
                withClue("Kavu Lair does not trigger for a power-3 creature") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }

        context("Essence Leak — conditional upkeep pay-or-sacrifice") {
            test("on a green creature, paying its mana cost on upkeep keeps it") {
                // P1 has enough untapped mana ({1}{G}) on the upkeep to pay Grizzly Bears'
                // own mana cost, so the granted ability offers the pay-or-sacrifice choice.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grizzly Bears")     // green, {1}{G}
                    .withCardInHand(1, "Essence Leak")
                    .withLandsOnBattlefield(1, "Island", 1)        // for the {U} cast
                    .withLandsOnBattlefield(1, "Forest", 2)        // untapped each upkeep → pays {1}{G}
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val castResult = game.castSpell(1, "Essence Leak", bears)
                withClue("Essence Leak casts onto Grizzly Bears: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Advance around the table to P1's NEXT upkeep ("your upkeep" = the
                // enchanted permanent's controller, P1). P2's upkeep en route does not trigger.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P1 postcombat (this turn)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)               // P2 upkeep
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P2 postcombat
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)               // P1 upkeep
                game.resolveStack()

                withClue("granted upkeep trigger pauses for pay-its-mana-cost-or-sacrifice") {
                    game.hasPendingDecision() shouldBe true
                }

                // Pay Grizzly Bears' own mana cost to keep it.
                game.answerYesNo(true)

                withClue("Grizzly Bears survives when its controller pays its mana cost") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("on a green creature, declining (or unable to pay) sacrifices it") {
                // P1 has no spare mana on the upkeep, so the granted ability auto-resolves
                // to its suffer clause: the enchanted permanent is sacrificed.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Grizzly Bears")     // green, {1}{G}
                    .withCardInHand(1, "Essence Leak")
                    .withLandsOnBattlefield(1, "Island", 1)        // only enough for the {U} cast
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Essence Leak", bears)
                game.resolveStack()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Grizzly Bears is sacrificed when its mana cost can't be paid") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("on a blue creature, the granted ability is inactive — no decision pends") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Storm Crow")        // blue (not red/green)
                    .withCardInHand(1, "Essence Leak")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crow = game.findPermanent("Storm Crow")!!
                val castResult = game.castSpell(1, "Essence Leak", crow)
                withClue("Essence Leak casts onto Storm Crow: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Advance to P1's NEXT upkeep, same as the green case.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P1 postcombat (this turn)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)               // P2 upkeep
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // P2 postcombat
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)               // P1 upkeep
                game.resolveStack()

                withClue("no decision pends — Storm Crow is neither red nor green") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
