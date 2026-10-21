package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.BrazenDwarf
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BrazenDwarfScenarioTest : FunSpec({
    val fourDice = card("Test Four Dice") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(DynamicAmount.Fixed(4), sides = 6) }
    }

    test("rolling four dice at once deals 1 damage to each opponent, once") {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(BrazenDwarf, fourDice))
            it.initMirrorMatch(Deck.of("Mountain" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!; val opponent = d.getOpponent(player)
        d.putPermanentOnBattlefield(player, "Brazen Dwarf")
        val source = d.putPermanentOnBattlefield(player, fourDice.name)
        d.submit(ActivateAbility(player, source, fourDice.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 19
        d.getLifeTotal(player) shouldBe 20
    }
})
