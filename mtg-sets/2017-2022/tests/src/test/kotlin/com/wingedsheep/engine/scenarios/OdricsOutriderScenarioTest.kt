package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Odric's Outrider (MID #29) — "Whenever this creature or another creature you control dies, put a
 * +1/+1 counter on target creature you control."
 */
class OdricsOutriderScenarioTest : ScenarioTestBase() {
    init {
        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("another creature you control dying puts a +1/+1 counter on target creature you control") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Odric's Outrider")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val outrider = game.findPermanent("Odric's Outrider")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("the death trigger asks for a creature you control") {
                (game.getPendingDecision() as? ChooseTargetsDecision).shouldNotBeNull()
            }
            game.selectTargets(listOf(outrider))
            game.resolveStack()

            counters(game, outrider) shouldBe 1
        }

        test("Odric's Outrider dying itself also triggers") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Odric's Outrider")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Doom Blade")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val outrider = game.findPermanent("Odric's Outrider")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(2, "Doom Blade", outrider).error shouldBe null
            game.resolveStack()

            withClue("Outrider's own death triggers it") {
                (game.getPendingDecision() as? ChooseTargetsDecision).shouldNotBeNull()
            }
            game.selectTargets(listOf(bears))
            game.resolveStack()

            game.isInGraveyard(1, "Odric's Outrider") shouldBe true
            counters(game, bears) shouldBe 1
        }
    }
}
