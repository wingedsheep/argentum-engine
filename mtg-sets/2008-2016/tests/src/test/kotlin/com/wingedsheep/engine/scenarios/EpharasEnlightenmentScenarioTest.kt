package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ephara's Enlightenment (BNG #146) — enters putting a +1/+1 counter on the enchanted creature,
 * grants flying, and may return to its owner's hand whenever a creature you control enters.
 */
class EpharasEnlightenmentScenarioTest : ScenarioTestBase() {

    private fun enchantBears(): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Ephara's Enlightenment")
            .withCardInHand(1, "Savannah Lions")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withLandsOnBattlefield(1, "Island", 2)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Ephara's Enlightenment", bears).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("enters with a +1/+1 counter on the enchanted creature, which gains flying") {
            val game = enchantBears()
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("the ETB trigger put one +1/+1 counter on the Bears") {
                game.state.getEntity(bears)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("a creature you control entering lets you return the Aura to hand") {
            val game = enchantBears()

            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Ephara's Enlightenment") shouldBe true
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the counter stays; flying leaves with the Aura") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
            }
        }

        test("declining keeps the Aura on the battlefield") {
            val game = enchantBears()

            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Ephara's Enlightenment") shouldBe true
        }
    }
}
