package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Starnheim Unleashed (KHM #33) — {2}{W}{W} Sorcery. "Create a 4/4 white Angel Warrior creature
 * token with flying and vigilance. If this spell was foretold, create X of those tokens instead."
 * Foretell {X}{X}{W}.
 *
 * The X exists only in the foretell cost: cast from hand there is no X and one Angel is made;
 * foretold and cast for {X}{X}{W}, it makes X Angels.
 */
class StarnheimUnleashedScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.angels(player: EntityId) = getCreatures(player).filter {
        state.projectedState.hasSubtype(it, "Angel") && state.projectedState.hasSubtype(it, "Warrior")
    }

    test("cast from hand, it creates one 4/4 Angel Warrior") {
        val d = driver()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Starnheim Unleashed")
        d.giveMana(p, Color.WHITE, 4)
        d.castSpell(p, spell).error shouldBe null
        d.bothPass()
        val angels = d.angels(p)
        angels.size shouldBe 1
        d.state.projectedState.getPower(angels.single()) shouldBe 4
        d.state.projectedState.getToughness(angels.single()) shouldBe 4
    }

    test("foretold and cast with X = 3, it creates three Angel Warriors") {
        val d = driver()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Starnheim Unleashed")
        d.giveMana(p, Color.WHITE, 2)
        d.submitSuccess(ForetellCard(p, spell))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p)
        d.giveMana(p, Color.WHITE, 7) // {X}{X}{W} with X = 3
        d.castXSpell(p, spell, xValue = 3).error shouldBe null
        d.bothPass()
        d.angels(p).size shouldBe 3
    }
})
