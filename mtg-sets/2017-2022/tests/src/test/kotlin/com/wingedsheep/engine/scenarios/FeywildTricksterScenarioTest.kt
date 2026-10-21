package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.FeywildTrickster
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FeywildTricksterScenarioTest : FunSpec({
    val threeDice = card("Test Three Dice") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(DynamicAmount.Fixed(3), sides = 20) }
    }
    val twoRolls = card("Test Two Rolls") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(sides = 6) then Effects.RollDice(DynamicAmount.Fixed(2), sides = 6) }
    }

    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(FeywildTrickster, threeDice, twoRolls))
        it.initMirrorMatch(Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activateAndResolve(controller: EntityId, roller: CardDefinition) {
        val source = putPermanentOnBattlefield(controller, roller.name)
        submit(ActivateAbility(controller, source, roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        bothPass()
        while (state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.faerieDragons(player: EntityId) =
        getPermanents(player).count { getCardName(it)?.startsWith("Faerie Dragon") == true }

    test("rolling three dice at once creates one Faerie Dragon") {
        val d = setup(); val player = d.activePlayer!!
        d.putPermanentOnBattlefield(player, "Feywild Trickster")
        d.activateAndResolve(player, threeDice)
        d.faerieDragons(player) shouldBe 1
    }

    test("two separate roll instructions in one ability create two Faerie Dragons") {
        val d = setup(); val player = d.activePlayer!!
        d.putPermanentOnBattlefield(player, "Feywild Trickster")
        d.activateAndResolve(player, twoRolls)
        d.faerieDragons(player) shouldBe 2
    }

    test("an opponent's roll does not trigger it") {
        val d = setup(); val player = d.activePlayer!!; val opponent = d.getOpponent(player)
        d.putPermanentOnBattlefield(player, "Feywild Trickster")
        d.passPriority(player)
        d.activateAndResolve(opponent, threeDice)
        d.faerieDragons(player) shouldBe 0
    }
})
