package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jump Scare ({W} instant) — Duskmourn: House of Horror.
 *
 * "Until end of turn, target creature gets +2/+2, gains flying, and becomes a Horror enchantment
 * creature in addition to its other types."
 *
 * Composed from atomic until-end-of-turn effects (ModifyStats + GrantKeyword + AddCreatureType +
 * AddCardType). Verifies all four pieces apply together and that they expire at end of turn.
 */
class JumpScareScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
    }

    test("grants +2/+2, flying, Horror subtype, and enchantment type until end of turn") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A vanilla 3/3 with no evasion to start.
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.state.projectedState.getPower(courser) shouldBe 3
        d.state.projectedState.getToughness(courser) shouldBe 3
        d.state.projectedState.hasKeyword(courser, Keyword.FLYING) shouldBe false
        d.state.projectedState.hasType(courser, "ENCHANTMENT") shouldBe false

        val spell = d.putCardInHand(you, "Jump Scare")
        d.giveMana(you, Color.WHITE, 1)
        d.castSpell(you, spell, targets = listOf(courser))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // +2/+2 → 5/5, flying, Horror creature subtype, and enchantment card type, still a creature.
        d.state.projectedState.getPower(courser) shouldBe 5
        d.state.projectedState.getToughness(courser) shouldBe 5
        d.state.projectedState.hasKeyword(courser, Keyword.FLYING) shouldBe true
        d.state.projectedState.hasSubtype(courser, "Horror") shouldBe true
        d.state.projectedState.hasType(courser, "ENCHANTMENT") shouldBe true
        d.state.projectedState.isCreature(courser) shouldBe true

        // Everything is "until end of turn" — pass into the next turn and the buffs are gone.
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.projectedState.getPower(courser) shouldBe 3
        d.state.projectedState.getToughness(courser) shouldBe 3
        d.state.projectedState.hasKeyword(courser, Keyword.FLYING) shouldBe false
        d.state.projectedState.hasSubtype(courser, "Horror") shouldBe false
        d.state.projectedState.hasType(courser, "ENCHANTMENT") shouldBe false
    }
})
