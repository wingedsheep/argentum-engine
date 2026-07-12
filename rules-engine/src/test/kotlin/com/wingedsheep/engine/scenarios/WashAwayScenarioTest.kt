package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Wash Away (VOW #87).
 *
 * {U} Instant — Cleave {1}{U}{U}
 * "Counter target spell [that wasn't cast from its owner's hand]."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * cast can only counter spells cast from somewhere *other* than their owner's hand (flashback,
 * foretell, adventure, cascade, …); the cleaved cast counters any spell.
 *
 * The printed restriction is enforced structurally via `StatePredicate.WasCastFromZone` (through
 * `TargetFilter.notCastFromZone(Zone.HAND)`), which reads the `castFromZone` the engine stamps at
 * cast time. Because a card in a hand is owned by that hand's player (CR 108.3), "wasn't cast from
 * its owner's hand" collapses to "wasn't cast from the HAND zone". These tests pin:
 *  - printed cast is a *legal* counter for a graveyard (flashback) cast and actually counters it,
 *  - printed cast is an *illegal* target against a hand-cast spell (the restriction holds), and
 *  - the cleaved cast counters a hand-cast spell (the restriction is lifted).
 */
class WashAwayScenarioTest : ScenarioTestBase() {

    init {
        context("Wash Away — printed cast (brackets present)") {

            test("counters a spell that wasn't cast from its owner's hand (a flashback cast)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wash Away")
                    .withLandsOnBattlefield(1, "Island", 1) // {U}
                    // Player 2 (active) has a flashback spell in the graveyard to re-cast.
                    .withCardInGraveyard(2, "Dreams of Laguna")
                    .withLandsOnBattlefield(2, "Island", 4) // Flashback {3}{U}
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(2)
                val libraryBefore = game.librarySize(2)

                // Player 2 flashback-casts Dreams of Laguna from the graveyard (castFromZone = GRAVEYARD).
                val flashback = game.castSpellFromGraveyard(2, "Dreams of Laguna")
                withClue("Flashback-casting Dreams of Laguna should succeed: ${flashback.error}") {
                    flashback.error shouldBe null
                }
                game.passPriority()

                // Player 1 responds with the printed Wash Away — legal because the spell wasn't cast from hand.
                val wash = game.castSpellTargetingStackSpell(1, "Wash Away", "Dreams of Laguna")
                withClue("A graveyard-cast spell is a legal target for the printed Wash Away: ${wash.error}") {
                    wash.error shouldBe null
                }
                game.resolveStack()

                // A countered flashback spell goes to the graveyard, not exile — flashback's "then
                // exile it" only applies on actual resolution (CR 702.34), which never happens here.
                withClue("Dreams of Laguna is countered: it goes to the graveyard, not the battlefield or exile") {
                    game.isInGraveyard(2, "Dreams of Laguna") shouldBe true
                    game.isInExile(2, "Dreams of Laguna") shouldBe false
                }
                withClue("Its surveil-then-draw never resolves, so neither hand nor library changed") {
                    game.handSize(2) shouldBe handBefore
                    game.librarySize(2) shouldBe libraryBefore
                }
            }

            test("rejects a spell cast from its owner's hand as an illegal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wash Away")
                    .withLandsOnBattlefield(1, "Island", 1) // {U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Grizzly Bears from hand (castFromZone = HAND).
                val bears = game.castSpell(2, "Grizzly Bears")
                withClue("Casting Grizzly Bears should succeed: ${bears.error}") {
                    bears.error shouldBe null
                }
                game.passPriority()

                // Player 1 tries the printed Wash Away — illegal, because the spell was cast from its owner's hand.
                val wash = game.castSpellTargetingStackSpell(1, "Wash Away", "Grizzly Bears")
                withClue("A hand-cast spell is not a legal target for the printed Wash Away") {
                    wash.error shouldNotBe null
                }

                game.resolveStack()
                withClue("Grizzly Bears resolves uncountered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Wash Away — cleaved cast (brackets removed)") {

            test("counters any spell, including one cast from its owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wash Away")
                    .withLandsOnBattlefield(1, "Island", 3) // Cleave {1}{U}{U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.castSpell(2, "Grizzly Bears")
                withClue("Casting Grizzly Bears should succeed: ${bears.error}") {
                    bears.error shouldBe null
                }
                game.passPriority()

                // Cleaved Wash Away lifts the "not from hand" restriction, so the hand-cast spell is legal.
                val wash = game.castSpellWithCleaveTargetingStackSpell(1, "Wash Away", "Grizzly Bears")
                withClue("The cleaved Wash Away may target a hand-cast spell: ${wash.error}") {
                    wash.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears is countered by the cleaved Wash Away") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
