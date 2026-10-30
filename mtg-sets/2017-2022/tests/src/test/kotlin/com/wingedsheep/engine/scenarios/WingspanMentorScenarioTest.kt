package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.iko.cards.WingspanMentor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wingspan Mentor (IKO #72) — {2}{U} Creature — Human Wizard 1/3.
 *
 *   "When this creature enters, put a flying counter on target non-Human creature you control.
 *    {2}{U}, {T}: Put a +1/+1 counter on each creature you control with flying."
 */
class WingspanMentorScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.counters(id: EntityId, type: CounterType) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

        test("ETB puts a flying counter on a non-Human creature you control — Humans aren't legal") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Wingspan Mentor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Elite Vanguard")   // Human Soldier
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val vanguard = game.findPermanent("Elite Vanguard")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Wingspan Mentor").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val options = decision.legalTargets.values.flatten()
            withClue("only a non-Human creature you control is legal") {
                options.contains(bears) shouldBe true
                options.contains(vanguard) shouldBe false
                options.contains(game.findPermanent("Wingspan Mentor")!!) shouldBe false
                options.contains(giant) shouldBe false
            }

            game.selectTargets(listOf(bears))
            game.resolveStack()

            game.counters(bears, CounterType.FLYING) shouldBe 1
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("{2}{U}, {T} puts a +1/+1 counter on each creature you control with flying, and only those") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Wingspan Mentor")
                .withCardOnBattlefield(1, "Wind Drake")       // 2/2 flier
                .withCardOnBattlefield(1, "Grizzly Bears")    // no flying
                .withCardOnBattlefield(2, "Birds of Paradise") // opponent's flier
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mentor = game.findPermanent("Wingspan Mentor")!!
            val myDrake = game.findPermanent("Wind Drake")!!
            val theirBirds = game.findPermanent("Birds of Paradise")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, mentor, WingspanMentor.activatedAbilities.single().id)
            ).error shouldBe null
            game.resolveStack()

            withClue("your flier grows; your ground creature and the opponent's flier don't") {
                game.counters(myDrake, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(myDrake) shouldBe 3
                game.counters(bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(theirBirds, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                game.counters(mentor, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }
        }
    }
}
