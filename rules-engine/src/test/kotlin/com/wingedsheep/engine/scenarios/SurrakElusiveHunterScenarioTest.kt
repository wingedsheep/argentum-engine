package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Surrak, Elusive Hunter ({2}{G}, 4/3).
 *
 * "Whenever a creature you control or a creature spell you control becomes the target of a spell
 * or ability an opponent controls, draw a card."
 *
 * The interesting case is the "creature spell you control" half: the engine must emit a
 * BecomesTargetEvent when an opponent's spell targets a creature spell on the stack, not only when
 * it targets a permanent. We verify both halves.
 */
class SurrakElusiveHunterScenarioTest : ScenarioTestBase() {

    init {
        context("Surrak, Elusive Hunter targeting triggers") {

            test("opponent targeting your creature SPELL draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Surrak, Elusive Hunter")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(2, "Exclude")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                // Player 1 casts a creature spell.
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.execute(PassPriority(game.player1Id))

                val creatureSpell = game.state.stack.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                // Player 2 targets the creature spell on the stack with Exclude.
                val exclude = game.state.getHand(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Exclude"
                }
                game.execute(
                    CastSpell(game.player2Id, exclude, listOf(ChosenTarget.Spell(creatureSpell)))
                ).error shouldBe null

                game.resolveStack()

                // The creature spell was countered, but Surrak's trigger drew a card.
                // Hand after: started with Surrak's draw (+1), minus the Grizzly Bears that left
                // the hand to the stack (-1) → net equal to handBefore. So check it specifically:
                // before = [Grizzly Bears], after = [the drawn Forest].
                withClue("Surrak should have drawn from the creature-spell-target trigger") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore
                }
                withClue("Grizzly Bears should be countered into the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("opponent targeting your creature PERMANENT draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Surrak, Elusive Hunter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    CastSpell(game.player2Id, shock(game), listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null

                game.resolveStack()

                withClue("Surrak should have drawn from the creature-permanent-target trigger") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore + 1
                }
            }

            test("opponent targeting a NON-creature you control does not draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Surrak, Elusive Hunter")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                // Shock can target any creature; target Surrak itself isn't "another" — that still
                // is a creature you control, so it WOULD draw. Instead target the land via a spell
                // that can — Shock only hits creatures/players, so target Player 1 directly: still
                // not a creature, so no draw.
                game.execute(
                    CastSpell(game.player2Id, shock(game), listOf(ChosenTarget.Player(game.player1Id)))
                ).error shouldBe null

                game.resolveStack()

                withClue("Targeting a player (not a creature you control) must not draw") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore
                }
            }

            // Regression guard: emitting BecomesTargetEvent for spell targets must not make
            // permanent-only "a creature you control becomes the target" triggers fire on a
            // creature SPELL. Pawpatch Recruit (no `includeSpellTargets`) must stay quiet when an
            // opponent counters a creature spell you control. Only Surrak opts into spell targets.
            test("permanent-only becomes-target triggers do NOT fire on a creature spell (Pawpatch Recruit)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pawpatch Recruit")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(2, "Exclude")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pawpatch = game.findPermanent("Pawpatch Recruit")!!

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.execute(PassPriority(game.player1Id))

                val creatureSpell = game.state.stack.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val exclude = game.state.getHand(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Exclude"
                }
                game.execute(
                    CastSpell(game.player2Id, exclude, listOf(ChosenTarget.Spell(creatureSpell)))
                ).error shouldBe null

                game.resolveStack()

                withClue("Pawpatch's trigger must not have fired (no pending counter-target decision)") {
                    game.state.pendingDecision shouldBe null
                }
                withClue("Pawpatch stays 2/1 — no wrongful +1/+1 counter from a spell target") {
                    game.state.projectedState.getPower(pawpatch) shouldBe 2
                }
                withClue("Grizzly Bears was countered into the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }

    private fun shock(game: TestGame) = game.state.getHand(game.player2Id).first { id ->
        game.state.getEntity(id)?.get<CardComponent>()?.name == "Shock"
    }
}
