package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.CyclopsOfEternalFury
import com.wingedsheep.mtg.sets.definitions.jou.cards.NyxInfusion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nyx Infusion — {2}{B} Aura.
 * Enchanted creature gets +2/+2 as long as it's an enchantment. Otherwise, it gets -2/-2.
 *
 * The sign of the buff hangs on the enchanted creature's projected card types.
 */
class NyxInfusionScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(NyxInfusion, CyclopsOfEternalFury))
        initMirrorMatch(deck = Deck.of("Swamp" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.infuse(player: EntityId, creature: EntityId) {
        val aura = putCardInHand(player, "Nyx Infusion")
        giveMana(player, Color.BLACK, 3)
        castSpellWithTargets(player, aura, listOf(ChosenTarget.Permanent(creature))).error shouldBe null
        resolveStack()
    }

    test("an enchantment creature gets +2/+2") {
        val d = driver()
        val me = d.activePlayer!!
        val cyclops = d.putCreatureOnBattlefield(me, "Cyclops of Eternal Fury")

        d.infuse(me, cyclops)

        d.state.projectedState.getPower(cyclops) shouldBe 7
        d.state.projectedState.getToughness(cyclops) shouldBe 5
    }

    test("a creature that isn't an enchantment gets -2/-2") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")

        d.infuse(me, courser)

        d.state.projectedState.getPower(courser) shouldBe 1
        d.state.projectedState.getToughness(courser) shouldBe 1
    }
})
