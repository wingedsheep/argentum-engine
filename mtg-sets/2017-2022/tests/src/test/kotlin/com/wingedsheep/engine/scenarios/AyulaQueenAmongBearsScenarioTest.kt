package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ayula, Queen Among Bears (MH1 #155) — {1}{G} Legendary Creature — Bear 2/2.
 *
 *   Whenever another Bear you control enters, choose one —
 *   • Put two +1/+1 counters on target Bear.
 *   • Target Bear you control fights target creature you don't control.
 */
class AyulaQueenAmongBearsScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ayula, Queen Among Bears")
        .withCardInHand(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("another Bear entering: put two +1/+1 counters on target Bear") {
            val game = setup()
            val ayula = game.findPermanent("Ayula, Queen Among Bears")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true

            game.chooseMode(0)
            game.selectTargets(listOf(ayula)).error shouldBe null
            game.resolveStack()

            plusOnes(game, ayula) shouldBe 2
            game.state.projectedState.getPower(ayula) shouldBe 4
            game.state.projectedState.getToughness(ayula) shouldBe 4
        }

        test("another Bear entering: target Bear you control fights target creature you don't control") {
            val game = setup()
            val elves = game.findPermanent("Llanowar Elves")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.chooseMode(1)
            val decisionId = game.getPendingDecision()!!.id
            game.submitDecision(
                TargetsResponse(decisionId, mapOf(0 to listOf(bears), 1 to listOf(elves)))
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a non-Bear creature entering does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ayula, Queen Among Bears")
                .withCardInHand(1, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Llanowar Elves").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }
    }
}
