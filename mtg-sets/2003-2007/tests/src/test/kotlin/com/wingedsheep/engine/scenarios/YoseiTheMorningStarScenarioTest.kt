package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Yosei, the Morning Star (CHK #50) — "When Yosei dies, target player skips their next untap step.
 * Tap up to five target permanents that player controls."
 */
class YoseiTheMorningStarScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)!!.has<TappedComponent>()

    private fun yoseiGame() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Yosei, the Morning Star")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInHand(1, "Murder")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withCardOnBattlefield(2, "Forest")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.killYosei() {
        castSpell(1, "Murder", targetId = findPermanent("Yosei, the Morning Star")!!).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Yosei, the Morning Star") {

            test("after choosing a player, only that player's permanents are offered, up to five") {
                val game = yoseiGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val forest = game.findPermanent("Forest")!!
                game.killYosei()

                val first = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                first.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.player1Id, game.player2Id)
                game.selectTargets(listOf(game.player2Id)).error shouldBe null

                val second = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("Bob's permanents only — Alice's Hill Giant and Swamps aren't offered") {
                    second.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, elves, forest)
                }
                second.targetRequirements.single().maxTargets shouldBe 5
                second.targetRequirements.single().minTargets shouldBe 0
                game.selectTargets(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                withClue("the two chosen permanents are tapped, the third isn't") {
                    game.isTapped(bears) shouldBe true
                    game.isTapped(forest) shouldBe true
                    game.isTapped(elves) shouldBe false
                }
                game.state.getEntity(game.player2Id)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("Bob skipped his untap step, so his permanents stayed tapped") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.isTapped(bears) shouldBe true
                    game.isTapped(forest) shouldBe true
                }
            }

            test("choosing no permanents still makes the player skip their next untap step") {
                val game = yoseiGame()
                game.killYosei()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.player2Id)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1
                game.isTapped(game.findPermanent("Grizzly Bears")!!) shouldBe false
            }

            test("a player with no permanents skips the empty permanents prompt") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Yosei, the Morning Star")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.killYosei()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                withClue("Bob controls nothing, so the trigger goes straight to the stack") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
                game.resolveStack()

                game.state.getEntity(game.player2Id)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1
            }

            test("choosing yourself offers only your own permanents") {
                val game = yoseiGame()
                val giant = game.findPermanent("Hill Giant")!!
                game.killYosei()
                game.selectTargets(listOf(game.player1Id)).error shouldBe null

                val second = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("Hill Giant and Alice's three Swamps — none of Bob's permanents") {
                    second.legalTargets[0]!!.size shouldBe 4
                    second.legalTargets[0]!!.contains(giant) shouldBe true
                    second.legalTargets[0]!!.contains(game.findPermanent("Grizzly Bears")!!) shouldBe false
                }
            }
        }
    }
}
