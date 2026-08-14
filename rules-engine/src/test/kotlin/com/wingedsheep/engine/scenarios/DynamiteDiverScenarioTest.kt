package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DynamiteDiverScenarioTest : FunSpec({
    val saddleThreeMount = card("Saddle Three Test Mount") {
        manaCost = "{3}"
        typeLine = "Creature — Horse Mount"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.saddle(3))
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(saddleThreeMount)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("its projected power plus two pays saddle") {
        val d = driver()
        val diver = d.putCreatureOnBattlefield(d.player1, "Dynamite Diver")
        val mount = d.putCreatureOnBattlefield(d.player1, "Saddle Three Test Mount")

        d.submitSuccess(SaddleMount(d.player1, mount, listOf(diver)))
        d.isTapped(diver) shouldBe true
    }
})
