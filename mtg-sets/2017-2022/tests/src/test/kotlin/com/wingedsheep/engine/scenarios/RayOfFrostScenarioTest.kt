package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ray of Frost.
 *
 * Both colour clauses read the *enchanted creature*, not the blue Aura: the enters trigger is an
 * intervening-if checked against the host the Aura just landed on, and the ability loss is a
 * conditional static. A red host is tapped and stripped; a non-red host keeps its abilities and
 * stays untapped, but is still locked out of its untap step.
 */
class RayOfFrostScenarioTest : ScenarioTestBase() {

    init {
        context("Ray of Frost") {

            test("a red creature is tapped and loses all abilities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ray of Frost")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dragon = game.findPermanent("Shivan Dragon")!!
                game.castSpell(1, "Ray of Frost", dragon).error shouldBe null
                game.resolveStack()

                withClue("the enchanted creature is red, so the enters trigger taps it") {
                    game.state.getEntity(dragon)?.has<TappedComponent>() shouldBe true
                }
                val projected = game.state.projectedState
                withClue("it is red, so it loses all abilities") {
                    projected.hasKeyword(dragon, Keyword.FLYING) shouldBe false
                    projected.hasLostAllAbilities(dragon) shouldBe true
                }
            }

            test("a non-red creature is not tapped and keeps its abilities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ray of Frost")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                game.castSpell(1, "Ray of Frost", drake).error shouldBe null
                game.resolveStack()

                withClue("the intervening-if fails for a blue creature, so it stays untapped") {
                    game.state.getEntity(drake)?.has<TappedComponent>() shouldBe false
                }
                val projected = game.state.projectedState
                withClue("it is not red, so it keeps flying") {
                    projected.hasKeyword(drake, Keyword.FLYING) shouldBe true
                    projected.hasLostAllAbilities(drake) shouldBe false
                }
            }

            test("the enchanted creature doesn't untap during its controller's untap step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Wind Drake", tapped = true)
                    .withCardAttachedTo(1, "Ray of Frost", "Wind Drake")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("Ray of Frost holds it down regardless of colour") {
                    game.state.getEntity(drake)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
