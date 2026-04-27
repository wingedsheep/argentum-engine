package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Winnowing.
 *
 * Card reference:
 * - Winnowing {4}{W}{W} — Sorcery (Convoke)
 *   "For each player, you choose a creature that player controls. Then each player
 *   sacrifices all other creatures they control that don't share a creature type
 *   with the chosen creature they control."
 *
 * Verifies the new [Chooser.SourceController] resolution: inside ForEachPlayer
 * iteration, the spell's caster makes the per-player creature selection (not the
 * iterated player, who would be the default "controller").
 */
class WinnowingScenarioTest : ScenarioTestBase() {

    private val soldierA = CardDefinition.creature(
        name = "WW Soldier A",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 1,
        toughness = 1
    )

    private val soldierB = CardDefinition.creature(
        name = "WW Soldier B",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.SOLDIER),
        power = 1,
        toughness = 1
    )

    private val goblin = CardDefinition.creature(
        name = "WW Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype.GOBLIN),
        power = 1,
        toughness = 1
    )

    private val wizard = CardDefinition.creature(
        name = "WW Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.WIZARD),
        power = 1,
        toughness = 1
    )

    private val beast = CardDefinition.creature(
        name = "WW Beast",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    init {
        cardRegistry.register(soldierA)
        cardRegistry.register(soldierB)
        cardRegistry.register(goblin)
        cardRegistry.register(wizard)
        cardRegistry.register(beast)

        context("Winnowing") {

            test("caster chooses a creature for each player; non-sharing creatures are sacrificed") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Winnowing")
                    .withCardOnBattlefield(1, "WW Soldier A")
                    .withCardOnBattlefield(1, "WW Soldier B")
                    .withCardOnBattlefield(1, "WW Goblin")
                    .withCardOnBattlefield(2, "WW Wizard")
                    .withCardOnBattlefield(2, "WW Beast")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Winnowing")
                game.resolveStack()

                // First pause: caster (P1) chooses one of P1's creatures.
                withClue("Expected pending decision for P1's per-player choice") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("Source controller (P1) should be the deciding player") {
                    game.getPendingDecision()!!.playerId shouldBe game.player1Id
                }
                val soldierAId = game.findPermanent("WW Soldier A")!!
                game.selectCards(listOf(soldierAId))

                // Goblin (no shared types with Soldier) is sacrificed; both Soldiers stay.
                withClue("Goblin (no Soldier subtype) should be sacrificed") {
                    game.isInGraveyard(1, "WW Goblin") shouldBe true
                }
                withClue("Soldier A (the chosen creature) should remain on battlefield") {
                    game.isOnBattlefield("WW Soldier A") shouldBe true
                }
                withClue("Soldier B (shares Soldier subtype with Soldier A) should remain") {
                    game.isOnBattlefield("WW Soldier B") shouldBe true
                }

                // Second pause: caster (P1) chooses one of P2's creatures.
                withClue("Expected pending decision for P2's per-player choice") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("Source controller (P1) should still be the deciding player") {
                    game.getPendingDecision()!!.playerId shouldBe game.player1Id
                }
                val wizardId = game.findPermanent("WW Wizard")!!
                game.selectCards(listOf(wizardId))
                game.resolveStack()

                // Beast (no shared types with Wizard) is sacrificed by P2.
                withClue("Beast (no Wizard/Human subtype) should be sacrificed") {
                    game.isInGraveyard(2, "WW Beast") shouldBe true
                }
                withClue("Wizard (the chosen creature) should remain on battlefield") {
                    game.isOnBattlefield("WW Wizard") shouldBe true
                }
            }

            test("creature sharing any subtype with chosen creature is spared") {
                // Soldier A has Human + Soldier subtypes.
                // Wizard has Human + Wizard subtypes.
                // Choosing Soldier A spares the Wizard via the shared "Human" type.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Winnowing")
                    .withCardOnBattlefield(1, "WW Soldier A") // Human Soldier — chosen
                    .withCardOnBattlefield(1, "WW Wizard")    // Human Wizard — shares Human
                    .withCardOnBattlefield(1, "WW Beast")     // Beast — no overlap
                    .withCardOnBattlefield(2, "WW Goblin")    // Lone P2 creature — auto-selected
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Winnowing")
                game.resolveStack()

                // P1 has 3 creatures → must choose. Pick Soldier A.
                game.hasPendingDecision() shouldBe true
                val soldierAId = game.findPermanent("WW Soldier A")!!
                game.selectCards(listOf(soldierAId))

                // P2 has only 1 creature → auto-selected (no decision).
                // The lone Goblin is the "chosen" for P2; nothing else to sacrifice for P2.
                game.hasPendingDecision() shouldBe false
                game.resolveStack()

                withClue("Wizard shares Human subtype with chosen Human Soldier — should survive") {
                    game.isOnBattlefield("WW Wizard") shouldBe true
                }
                withClue("Beast shares no subtype with chosen Human Soldier — should be sacrificed") {
                    game.isInGraveyard(1, "WW Beast") shouldBe true
                }
                withClue("P2's only creature was auto-chosen — Goblin remains") {
                    game.isOnBattlefield("WW Goblin") shouldBe true
                }
            }
        }
    }
}
