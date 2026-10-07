package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.ShimmerdriftVale
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ShimmerdriftValeScenarioTest : FunSpec({
    for (chosenColor in Color.entries) {
        test("enters tapped then produces snow mana of the chosen $chosenColor without another choice") {
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + listOf(ShimmerdriftVale))
                initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
            }
            val you = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val vale = d.putCardInHand(you, "Shimmerdrift Vale")
            d.playLand(you, vale)
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
            d.submitDecision(you, ColorChosenResponse(decision.id, chosenColor))
            d.isTapped(vale) shouldBe true

            d.untapPermanent(vale)
            val result = d.submit(ActivateAbility(you, vale, ShimmerdriftVale.activatedAbilities.single().id))
            result.outcome shouldBe Outcome.Done
            d.isPaused shouldBe false
            d.isTapped(vale) shouldBe true
            val pool = d.state.getEntity(you)!!.get<ManaPoolComponent>()!!
            pool.getAmount(chosenColor) shouldBe 1
            pool.snowMana shouldBe mapOf(chosenColor to 1)
            pool.restrictedMana.size shouldBe 0
        }
    }
})
