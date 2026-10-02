package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `Gate.MayPayAnyAmountOfLife` with an `otherwise` branch — "If you do, [then]. Otherwise, [otherwise]."
 * Declining (choosing 0) is "you don't", so it must run `otherwise` just like the unpayable fall-through.
 */
private val LifeBargain = card("Life Bargain") {
    manaCost = "{2}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, you may pay any amount of life. If you do, draw that " +
        "many cards. Otherwise, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = GatedEffect(
            gate = Gate.MayPayAnyAmountOfLife,
            then = Effects.DrawCards(DynamicAmounts.xValue()),
            otherwise = Effects.GainLife(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "T1"
    }
}

class MayPayAnyAmountOfLifeGateTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(LifeBargain))

        fun game() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Life Bargain")
            .withLifeTotal(1, 20)
            .apply { repeat(3) { withCardInLibrary(1, "Swamp") } }
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("paying runs then with X bound, not otherwise") {
            val game = game()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            game.chooseNumber(2).error shouldBe null

            game.getLifeTotal(1) shouldBe 18
            game.handSize(1) shouldBe 2
        }

        test("choosing 0 declines and runs otherwise") {
            val game = game()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            game.chooseNumber(0).error shouldBe null

            game.getLifeTotal(1) shouldBe 21
            game.handSize(1) shouldBe 0
        }
    }
}
