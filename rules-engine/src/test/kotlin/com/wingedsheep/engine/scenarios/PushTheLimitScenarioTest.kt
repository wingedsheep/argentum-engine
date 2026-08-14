package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Push the Limit (DFT #143).
 *
 * Push the Limit {5}{R}{R} — Sorcery
 * Return all Mount and Vehicle cards from your graveyard to the battlefield. Sacrifice them at the
 * beginning of the next end step.
 * Vehicles you control become artifact creatures until end of turn. Creatures you control gain
 * haste until end of turn.
 *
 * Three claims worth pinning:
 *
 *  1. The reanimation is filtered — only Mount and Vehicle cards come back.
 *  2. The clause order matters. The Vehicles are animated *after* they return, and haste is granted
 *     *after* that, so a Vehicle that came back this turn is a hasty creature and can attack.
 *  3. The sacrifice actually fires. It is scheduled as one delayed trigger per returned permanent
 *     with the entity baked in, so a whole-pipeline reference going stale would show up here as
 *     permanents that quietly survive the end step.
 */
class PushTheLimitScenarioTest : ScenarioTestBase() {

    init {
        context("Push the Limit") {

            test("returns only Mounts and Vehicles, animates them, and gives everything haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Push the Limit")
                    .withCardInGraveyard(1, "Marshals' Pathcruiser") // Artifact — Vehicle 6/5
                    .withCardInGraveyard(1, "Guardian Sunmare") // Creature — Horse Mount 5/5
                    .withCardInGraveyard(1, "Centaur Courser") // plain creature — must stay put
                    .withCardOnBattlefield(1, "Grizzly Bears") // already in play, gains haste too
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Push the Limit")
                withClue("cast should succeed: ${castResult.error}") { castResult.error shouldBe null }
                game.resolveStack()

                withClue("the Vehicle and the Mount came back") {
                    game.isOnBattlefield("Marshals' Pathcruiser") shouldBe true
                    game.isOnBattlefield("Guardian Sunmare") shouldBe true
                }
                withClue("a plain creature card is neither a Mount nor a Vehicle — it stays dead") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }

                val pathcruiser = game.findPermanent("Marshals' Pathcruiser")!!
                val sunmare = game.findPermanent("Guardian Sunmare")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("the returned Vehicle is animated for the turn") {
                    projected.isCreature(pathcruiser) shouldBe true
                    projected.hasType(pathcruiser, "ARTIFACT") shouldBe true
                }
                withClue("haste is granted after the animation, so the Vehicle gets it too") {
                    projected.hasKeyword(pathcruiser, Keyword.HASTE) shouldBe true
                }
                withClue("the returned Mount and the pre-existing creature also gain haste") {
                    projected.hasKeyword(sunmare, Keyword.HASTE) shouldBe true
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("every returned permanent is sacrificed at the beginning of the next end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Push the Limit")
                    .withCardInGraveyard(1, "Marshals' Pathcruiser")
                    .withCardInGraveyard(1, "Guardian Sunmare")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Push the Limit").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Marshals' Pathcruiser") shouldBe true
                game.isOnBattlefield("Guardian Sunmare") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("both returned permanents are sacrificed, not just the first one") {
                    game.isOnBattlefield("Marshals' Pathcruiser") shouldBe false
                    game.isOnBattlefield("Guardian Sunmare") shouldBe false
                    game.isInGraveyard(1, "Marshals' Pathcruiser") shouldBe true
                    game.isInGraveyard(1, "Guardian Sunmare") shouldBe true
                }
                withClue("a creature that was already in play is untouched by the sacrifice") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("with an empty graveyard the spell still resolves and grants haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Push the Limit")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Push the Limit").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("nothing to reanimate is not a failure — the last two clauses still apply") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
