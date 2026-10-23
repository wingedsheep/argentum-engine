package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PixieGuide
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pixie Guide (AFR #66): "If you would roll one or more dice, instead roll that many dice plus one
 * and ignore the lowest roll" — only for its controller.
 */
class PixieGuideScenarioTest : FunSpec({
    val twoD6 = card("Test Two d6 Roller") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(DynamicAmount.Fixed(2), sides = 6) }
    }

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(PixieGuide, twoD6))
        it.initMirrorMatch(Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.rollTwo(player: EntityId): List<DieRolledEvent> {
        val source = putPermanentOnBattlefield(player, twoD6.name)
        replaceState(state.copy(rng = GameRng.seeded(66)))
        submit(ActivateAbility(player, source, twoD6.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        return bothPass().events.filterIsInstance<DieRolledEvent>()
    }

    test("its controller rolls two dice as three and ignores the lowest") {
        val d = setup()
        val player = d.activePlayer!!
        val pixie = d.putPermanentOnBattlefield(player, "Pixie Guide")
        d.state.projectedState.hasKeyword(pixie, Keyword.FLYING) shouldBe true
        val rolls = d.rollTwo(player)
        rolls.size shouldBe 3
        rolls.single { it.ignored }.result shouldBe rolls.minOf { it.result }
    }

    test("an opponent's Pixie Guide does not change your roll") {
        val d = setup()
        val player = d.activePlayer!!
        d.putPermanentOnBattlefield(d.getOpponent(player), "Pixie Guide")
        val rolls = d.rollTwo(player)
        rolls.size shouldBe 2
        rolls.none { it.ignored } shouldBe true
    }
})
