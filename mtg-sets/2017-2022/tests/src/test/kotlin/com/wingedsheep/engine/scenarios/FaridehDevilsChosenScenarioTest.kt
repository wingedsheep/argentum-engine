package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.FaridehDevilsChosen
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FaridehDevilsChosenScenarioTest : FunSpec({
    val twoD20 = card("Test Two d20") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(DynamicAmount.Fixed(2), sides = 20) }
    }

    /** First seed whose two d20 results satisfy [accept]. */
    fun seedFor(accept: (List<Int>) -> Boolean): Long = (0L..10_000L).first { seed ->
        var rng = GameRng.seeded(seed)
        val results = List(2) { val (face, next) = rng.nextInt(20); rng = next; face + 1 }
        accept(results)
    }

    for ((label, accept, draws) in listOf(
        Triple("a 10 or higher among the results draws a card", { r: List<Int> -> r[0] >= 10 && r[1] < 10 }, 1),
        Triple("results all below 10 draw nothing", { r: List<Int> -> r.max() < 10 }, 0),
    )) test(label) {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(FaridehDevilsChosen, twoD20))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val farideh = d.putPermanentOnBattlefield(player, "Farideh, Devil's Chosen")
        val source = d.putPermanentOnBattlefield(player, twoD20.name)
        d.submit(ActivateAbility(player, source, twoD20.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        d.replaceState(d.state.copy(rng = GameRng.seeded(seedFor(accept))))
        d.bothPass()
        d.state.stack.size shouldBe 1
        val hand = d.getHand(player).size
        d.bothPass()
        d.getHand(player).size shouldBe hand + draws
        d.state.projectedState.hasKeyword(farideh, Keyword.FLYING) shouldBe true
        d.state.projectedState.hasKeyword(farideh, Keyword.MENACE) shouldBe true
    }
})
