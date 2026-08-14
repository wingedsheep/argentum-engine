package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Tenacious Tomeseeker. */
class TenaciousTomeseekerScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Tenacious Tomeseeker — bargain-gated graveyard return") {
            test("bargained, it returns the targeted instant from your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tenacious Tomeseeker")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardInGraveyard(1, "Candy Grapple")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellBargained(
                    1,
                    "Tenacious Tomeseeker",
                    sacrificeName = "A Tale for the Ages",
                ).error shouldBe null
                game.resolveStack()

                // The graveyard target belongs to the enters trigger, chosen once that trigger is
                // put on the stack — i.e. only on the bargained branch (CR 702.166d).
                val grapple = game.findCardsInGraveyard(1, "Candy Grapple").single()
                game.selectTargets(listOf(grapple)).error shouldBe null
                game.resolveStack()

                withClue("the bargained enters trigger returned the instant to hand") {
                    game.isInHand(1, "Candy Grapple") shouldBe true
                    game.isInGraveyard(1, "Candy Grapple") shouldBe false
                }
            }

            test("unbargained, the enters trigger never goes on the stack (CR 603.4)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tenacious Tomeseeker")
                    .withCardInGraveyard(1, "Candy Grapple")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tenacious Tomeseeker").error shouldBe null
                game.resolveStack()

                withClue("the creature resolved") {
                    game.isOnBattlefield("Tenacious Tomeseeker") shouldBe true
                }
                withClue("no bargain, so nothing came back and no decision was raised") {
                    game.isInGraveyard(1, "Candy Grapple") shouldBe true
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
