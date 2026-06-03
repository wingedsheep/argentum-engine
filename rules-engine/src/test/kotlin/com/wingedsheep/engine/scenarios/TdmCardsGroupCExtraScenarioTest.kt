package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Tarkir: Dragonstorm cards:
 *   Ureni's Rebuff (#63), Sarkhan's Resolve (#158),
 *   Rally the Monastery (#19), Riling Dawnbreaker // Signaling Roar (#21).
 *
 * Each card composes existing engine mechanics; these tests prove the composed
 * behaviour end-to-end (bounce, modal pump/destroy, conditional cost reduction,
 * prowess token creation, and the Omen token face).
 *
 * Named with an "Extra" suffix to avoid colliding with another agent's
 * TdmGroupCScenarioTest (a different batch of TDM spells).
 */
class TdmCardsGroupCExtraScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Ureni's Rebuff") {
            test("returns target creature to its owner's hand") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Ureni's Rebuff")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Ureni's Rebuff").first()
                val bear = game.findPermanent("Grizzly Bears")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.findCardsInHand(2, "Grizzly Bears").size shouldBe 1
            }
        }

        context("Sarkhan's Resolve") {
            test("mode 0 gives +3/+3 until end of turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Sarkhan's Resolve")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Sarkhan's Resolve").first()
                val bear = game.findPermanent("Grizzly Bears")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(ChosenTarget.Permanent(bear)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                projector.getProjectedPower(game.state, bear) shouldBe 5
                projector.getProjectedToughness(game.state, bear) shouldBe 5
            }

            test("mode 1 destroys a creature with flying") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Sarkhan's Resolve")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Wind Drake") // 2/2 flyer
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Sarkhan's Resolve").first()
                val drake = game.findPermanent("Wind Drake")!!

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(ChosenTarget.Permanent(drake)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(drake)))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Wind Drake") shouldBe false
                game.graveyardSize(2) shouldBe 1
            }
        }

        context("Rally the Monastery") {
            test("mode 0 creates two 1/1 Monk tokens with prowess") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Rally the Monastery")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Rally the Monastery").first()
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = spell, chosenModes = listOf(0))
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanents("Monk").size shouldBe 2
            }

            test("cost reduction applies after casting another spell this turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Rally the Monastery")
                    .withCardInHand(1, "Armored Pegasus") // a vanilla {1}{W} spell cast earlier this turn
                    .withLandsOnBattlefield(1, "Plains", 4) // {1}{W} for Pegasus + {1}{W} for the reduced Rally
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast a vanilla creature first so "you've cast another spell this turn" is true.
                val pegasus = game.findCardsInHand(1, "Armored Pegasus").first()
                val firstCast = game.execute(CastSpell(playerId = game.player1Id, cardId = pegasus))
                withClue("Pegasus should cast: ${firstCast.error}") { firstCast.error shouldBe null }
                game.resolveStack()

                val spell = game.findCardsInHand(1, "Rally the Monastery").first()
                // {3}{W} reduced by {2} → {1}{W}; only two Plains remain after Pegasus.
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = spell, chosenModes = listOf(0))
                )
                withClue("Reduced spell should be castable with the remaining Plains: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                game.findPermanents("Monk").size shouldBe 2
            }
        }

        context("Riling Dawnbreaker // Signaling Roar") {
            test("Omen face creates a 2/2 Soldier token and shuffles back into the library") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Riling Dawnbreaker")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Riling Dawnbreaker").first()
                val libraryBefore = game.librarySize(1)

                // Omen face is faceIndex = 0.
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, faceIndex = 0)
                )
                withClue("Casting Signaling Roar should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                game.findPermanents("Soldier").size shouldBe 1
                withClue("Omen shuffles back into the library, not the graveyard/battlefield") {
                    game.findCardsInLibrary(1, "Riling Dawnbreaker").size shouldBe 1
                    game.librarySize(1) shouldBe libraryBefore + 1
                    game.isOnBattlefield("Riling Dawnbreaker") shouldBe false
                }
            }
        }
    }
}
