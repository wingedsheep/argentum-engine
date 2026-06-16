package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FleetingReflection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fleeting Reflection (OTJ #49) — {1}{U} Instant.
 *
 * "Target creature you control gains hexproof until end of turn. Untap that creature. Until end of
 * turn, it becomes a copy of up to one other target creature."
 *
 * Exercises the new single-permanent copy shape:
 * [com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect.affected].
 */
class FleetingReflectionScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(FleetingReflection)
        d.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("creature gains hexproof, untaps, and becomes a copy of the other target") {
        val d = newDriver()
        val p1 = d.player1

        val spell = d.putCardInHand(p1, "Fleeting Reflection")
        // My creature: Savannah Lions (1/1). Copy source: Centaur Courser (3/3).
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.tapPermanent(lions)
        d.giveMana(p1, Color.BLUE, 1)
        d.giveColorlessMana(p1, 1)

        d.castSpellWithTargets(
            p1, spell,
            listOf(ChosenTarget.Permanent(lions), ChosenTarget.Permanent(courser))
        ).error shouldBe null
        d.bothPass()

        d.isPaused shouldBe false
        d.isTapped(lions) shouldBe false // untapped

        val projected = projector.project(d.state)
        projected.hasKeyword(lions, Keyword.HEXPROOF) shouldBe true
        // Became a copy of Centaur Courser (3/3).
        projected.getPower(lions) shouldBe 3
        projected.getToughness(lions) shouldBe 3
    }

    test("with no second target chosen, creature only gains hexproof and untaps") {
        val d = newDriver()
        val p1 = d.player1

        val spell = d.putCardInHand(p1, "Fleeting Reflection")
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.tapPermanent(lions)
        d.giveMana(p1, Color.BLUE, 1)
        d.giveColorlessMana(p1, 1)

        // Optional second target omitted (only the first target is chosen).
        d.castSpellWithTargets(p1, spell, listOf(ChosenTarget.Permanent(lions))).error shouldBe null
        d.bothPass()

        d.isPaused shouldBe false
        d.isTapped(lions) shouldBe false

        val projected = projector.project(d.state)
        projected.hasKeyword(lions, Keyword.HEXPROOF) shouldBe true
        // No copy: still 1/1.
        projected.getPower(lions) shouldBe 1
        projected.getToughness(lions) shouldBe 1
    }
})
