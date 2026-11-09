package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Declaration in Stone (SOI #12).
 *
 *   Exile target creature and all other creatures its controller controls with the same name as
 *   that creature. That player investigates for each nontoken creature exiled this way.
 *
 * Pins the sweep's scope (only the target's controller's namesakes), the investigate count (tokens
 * exiled this way don't count) and its recipient (the target's controller, not the caster), and the
 * face-down ruling: a face-down creature has no name, so it neither drags its real-name siblings
 * along as the target nor gets swept up as a namesake.
 */
class DeclarationInStoneScenarioTest : ScenarioTestBase() {

    private fun TestGame.cluesOf(playerId: EntityId): Int =
        findPermanents("Clue").count { state.projectedState.getController(it) == playerId }

    private fun TestGame.bobsBears(): List<EntityId> =
        findPermanents("Grizzly Bears").filter { state.projectedState.getController(it) == player2Id }

    init {
        context("Declaration in Stone") {

            test("exiles the target and its controller's namesakes; that player investigates per nontoken") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Declaration in Stone")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.bobsBears().first()

                game.castSpell(1, "Declaration in Stone", target).error shouldBe null
                game.resolveStack()

                withClue("all three of Bob's Grizzly Bears, the token included, are gone") {
                    game.bobsBears().size shouldBe 0
                }
                withClue("Alice's own Grizzly Bears is not her opponent's namesake") {
                    val survivor = game.findPermanents("Grizzly Bears").single()
                    game.state.projectedState.getController(survivor) shouldBe game.player1Id
                }
                withClue("a creature with a different name stays") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Bob investigates once per nontoken creature exiled — the token doesn't count") {
                    game.cluesOf(game.player2Id) shouldBe 2
                    game.cluesOf(game.player1Id) shouldBe 0
                }
            }

            test("a face-down namesake is not swept up") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Declaration in Stone")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (target, hidden) = game.bobsBears()
                game.state = game.state.updateEntity(hidden) { it.with(FaceDownComponent) }

                game.castSpell(1, "Declaration in Stone", target).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield().contains(target) shouldBe false
                withClue("the face-down Grizzly Bears has no name, so it shares none with the target") {
                    game.state.getBattlefield().contains(hidden) shouldBe true
                }
                game.cluesOf(game.player2Id) shouldBe 1
            }

            test("a face-down target is exiled alone") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Declaration in Stone")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (target, faceUp) = game.bobsBears()
                game.state = game.state.updateEntity(target) { it.with(FaceDownComponent) }

                game.castSpell(1, "Declaration in Stone", target).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield().contains(target) shouldBe false
                withClue("the face-down target has no name, so its face-up real-name sibling stays") {
                    game.state.getBattlefield().contains(faceUp) shouldBe true
                }
                withClue("the face-down creature was a nontoken creature exiled this way") {
                    game.cluesOf(game.player2Id) shouldBe 1
                }
            }
        }
    }
}
