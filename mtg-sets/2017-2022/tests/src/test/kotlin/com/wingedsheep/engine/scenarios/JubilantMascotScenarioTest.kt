package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jubilant Mascot (BBD #28) — {2}{W} Creature — Homunculus 1/1.
 *
 * "At the beginning of combat on your turn, you may pay {3}{W}. If you do, support 2. (Put a
 * +1/+1 counter on each of up to two other target creatures.)"
 *
 * Ruling (2018-06-08): the support targets are chosen when the trigger is put on the stack; whether
 * to pay {3}{W} is chosen only as it resolves.
 */
class JubilantMascotScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.plusOnes(id: EntityId) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        fun TestGame.tappedPlains() =
            findAllPermanents("Plains").count { state.getEntity(it)?.has<TappedComponent>() == true }

        fun game() = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Jubilant Mascot")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("targets are chosen at trigger time, then paying {3}{W} supports both") {
            val game = game()
            val mascot = game.findPermanent("Jubilant Mascot")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

            val targeting = withClue("support targets are chosen as the trigger goes on the stack") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            withClue("support targets only *other* creatures, any controller") {
                val legal = targeting.legalTargets.values.flatten()
                legal shouldContain bears
                legal shouldContain lions
                legal shouldNotContain mascot
            }
            game.submitDecision(TargetsResponse(targeting.id, mapOf(0 to listOf(bears, lions)))).error shouldBe null
            game.tappedPlains() shouldBe 0

            game.resolveStack()
            withClue("the {3}{W} is offered only as the ability resolves") {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }
            game.answerYesNo(true).error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            game.tappedPlains() shouldBe 4
            game.plusOnes(bears) shouldBe 1
            game.plusOnes(lions) shouldBe 1
            game.plusOnes(mascot) shouldBe 0
        }

        test("declining the payment puts no counters and spends no mana") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            val targeting = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.submitDecision(TargetsResponse(targeting.id, mapOf(0 to listOf(bears)))).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.plusOnes(bears) shouldBe 0
            game.tappedPlains() shouldBe 0
        }
    }
}
