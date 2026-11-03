package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dr. Leonard "Bones" McCoy (TRK #197) — {1}{G}{G} Legendary Creature — Human Doctor 3/2, Flash.
 *
 *   When Dr. McCoy enters, choose one —
 *   • Return target non-Doctor permanent card from your graveyard to your hand.
 *   • Choose up to one other target creature. When it dies this turn, return that card to the
 *     battlefield under its owner's control with a +1/+1 counter on it at the beginning of the
 *     next end step.
 */
class DrLeonardBonesMcCoyScenarioTest : ScenarioTestBase() {

    private val mccoy = "Dr. Leonard \"Bones\" McCoy"

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun setup(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, mccoy)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInGraveyard(1, "Hill Giant")
            .withCardInGraveyard(1, "Dr. McCoy, Surly Surgeon")
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) {
            builder.withCardInLibrary(1, "Forest")
            builder.withCardInLibrary(2, "Forest")
        }
        return builder.build()
    }

    init {
        test("mode one returns a non-Doctor permanent card; Doctors and nonpermanents can't be targeted") {
            val game = setup()
            game.castSpell(1, mccoy).error shouldBe null
            game.resolveStack()
            game.chooseMode(0)

            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].orEmpty()
            legal shouldContain giant
            legal shouldNotContain game.findCardsInGraveyard(1, "Dr. McCoy, Surly Surgeon").single()
            legal shouldNotContain game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Hill Giant") shouldBe true
        }

        test("mode two: the creature dies this turn and returns at the next end step under its owner with a +1/+1 counter") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, mccoy).error shouldBe null
            game.resolveStack()
            game.chooseMode(1)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            withClue("it stays in the graveyard until the end step") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears")
            returned shouldNotBe null
            game.state.projectedState.getController(returned!!) shouldBe game.player2Id
            plusOnes(game, returned) shouldBe 1
        }

        test("mode two with no target chosen does nothing") {
            val game = setup()
            game.castSpell(1, mccoy).error shouldBe null
            game.resolveStack()
            game.chooseMode(1)
            game.selectTargets(emptyList()).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
        }
    }
}
