package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.InvasionOfMoag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of Moag // Bloomwielder Dryads.
 *
 * Front: entering puts a +1/+1 counter on each creature you control (and none on the opponent's).
 * Back: ward {2}, and a +1/+1 counter on target creature you control at your end step.
 */
class InvasionOfMoagScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("front: entering puts a +1/+1 counter on each creature you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Moag")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Moag").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 4
            withClue("the opponent's creature gets nothing") {
                projected.getPower(game.findPermanent("Centaur Courser")!!) shouldBe 3
            }
        }

        test("back: Bloomwielder Dryads has ward 2 and counters a creature at your end step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Moag")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Moag")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val dryads = game.findPermanent("Bloomwielder Dryads")
            dryads shouldNotBe null
            game.state.projectedState.getPower(dryads!!) shouldBe 3
            InvasionOfMoag.backFace!!.keywordAbilities.any { it is KeywordAbility.Ward } shouldBe true

            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            counters(game, bears) shouldBe 1
            counters(game, dryads) shouldBe 0
        }
    }
}
