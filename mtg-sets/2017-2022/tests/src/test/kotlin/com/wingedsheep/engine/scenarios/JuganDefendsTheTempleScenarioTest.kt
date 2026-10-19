package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jugan Defends the Temple // Remnant of the Rising Star (NEO #194).
 *
 * Plays the Saga through: chapter I's mana-producing Monk, chapter II's counters on up to two
 * targets, chapter III's flip. Then the Remnant's pay-{X} reflexive trigger puts the counters on
 * the creature that entered (not on the Remnant), and the fifth modified creature turns on the
 * +5/+5 and trample.
 */
class JuganDefendsTheTempleScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toMyNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        fun TestGame.plusOnes(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Jugan Defends the Temple") {

            test("chapters I-III, then the Remnant's pay-X counters and five-modified anthem") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Jugan Defends the Temple")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 9)
                    .apply { repeat(3) { withCardInHand(1, "Grizzly Bears") } }
                    .apply { repeat(8) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val firstBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Jugan Defends the Temple").error shouldBe null
                game.resolveStack()

                val monk = game.state.getBattlefield().firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name?.startsWith("Human Monk") == true
                }
                withClue("chapter I created a Human Monk token") { monk shouldNotBe null }

                game.toMyNextMain()
                withClue("chapter II asks for up to two target creatures") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                    game.selectTargets(listOf(monk!!, firstBears)).error shouldBe null
                }
                game.resolveStack()
                withClue("each target got a +1/+1 counter") {
                    game.plusOnes(monk!!) shouldBe 1
                    game.plusOnes(firstBears) shouldBe 1
                }

                game.toMyNextMain()
                game.resolveStack()
                withClue("chapter III returned the Saga transformed") {
                    game.findPermanent("Jugan Defends the Temple") shouldBe null
                    game.isOnBattlefield("Remnant of the Rising Star") shouldBe true
                }
                val remnant = game.findPermanent("Remnant of the Rising Star")!!
                game.state.projectedState.hasKeyword(remnant, Keyword.FLYING) shouldBe true

                // Cast three Bears, paying X = 1 for each: modified creatures go 3 -> 4 -> 5.
                repeat(3) { i ->
                    val before = game.findPermanents("Grizzly Bears").toSet()
                    game.castSpell(1, "Grizzly Bears").error shouldBe null
                    game.resolveStack()
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                    game.chooseNumber(1).error shouldBe null
                    game.resolveStack()

                    val entered = (game.findPermanents("Grizzly Bears").toSet() - before).single()
                    withClue("the X counters land on the creature that entered, not the Remnant") {
                        game.plusOnes(entered) shouldBe 1
                        game.plusOnes(remnant) shouldBe 0
                    }

                    val anthemOn = i == 2
                    withClue("after Bears #${i + 1}, five modified creatures = $anthemOn") {
                        game.state.projectedState.getPower(remnant) shouldBe if (anthemOn) 7 else 2
                        game.state.projectedState.getToughness(remnant) shouldBe if (anthemOn) 7 else 2
                        game.state.projectedState.hasKeyword(remnant, Keyword.TRAMPLE) shouldBe anthemOn
                    }
                }
            }
        }
    }
}
