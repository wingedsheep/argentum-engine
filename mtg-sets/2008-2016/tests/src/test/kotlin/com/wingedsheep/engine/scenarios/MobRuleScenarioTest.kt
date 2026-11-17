package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.MobRule
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mob Rule (FRF #109) — "Choose one — • Gain control of all creatures with power 4 or greater until
 * end of turn. Untap those creatures. They gain haste until end of turn. • Gain control of all
 * creatures with power 3 or less until end of turn. Untap those creatures. They gain haste until end
 * of turn."
 *
 * Each mode's power filter picks "those creatures" once: a creature whose power changes because it
 * changed sides (an anthem the caster controls) is still untapped and given haste.
 */
class MobRuleScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(MobRule, GloriousAnthem))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castMobRule(mode: Int) {
        val mobRule = putCardInHand(player1, "Mob Rule")
        giveMana(player1, Color.RED, 6)
        submit(
            CastSpell(player1, mobRule, paymentStrategy = PaymentStrategy.FromPool, chosenModes = listOf(mode))
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("power 4 or greater mode steals, untaps and hastes only the big creatures") {
        val d = setup()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val force = d.putCreatureOnBattlefield(p2, "Force of Nature")
        val courser = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        d.tapPermanent(force)
        d.tapPermanent(courser)

        d.castMobRule(mode = 0)

        val projected = d.state.projectedState
        withClue("the 5/5 changes sides, untaps and gains haste") {
            projected.getController(force) shouldBe p1
            d.isTapped(force) shouldBe false
            projected.hasKeyword(force, Keyword.HASTE) shouldBe true
        }
        withClue("the 3/3 is untouched") {
            projected.getController(courser) shouldBe p2
            d.isTapped(courser) shouldBe true
            projected.hasKeyword(courser, Keyword.HASTE) shouldBe false
        }
    }

    test("power 3 or less mode keeps hold of a creature an anthem pumps past 3 after it changes sides") {
        val d = setup()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        d.putPermanentOnBattlefield(p1, "Glorious Anthem")
        val courser = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        val force = d.putCreatureOnBattlefield(p2, "Force of Nature")
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions")
        d.tapPermanent(courser)
        d.tapPermanent(force)
        d.tapPermanent(lions)

        d.castMobRule(mode = 1)

        val projected = d.state.projectedState
        withClue("the Courser is mine, now a 4/4, and still untapped with haste") {
            projected.getController(courser) shouldBe p1
            projected.getPower(courser) shouldBe 4
            d.isTapped(courser) shouldBe false
            projected.hasKeyword(courser, Keyword.HASTE) shouldBe true
        }
        withClue("my own 3-power Lions are untapped and hasted too") {
            d.isTapped(lions) shouldBe false
            projected.hasKeyword(lions, Keyword.HASTE) shouldBe true
        }
        withClue("the 5/5 stays with its owner, tapped") {
            projected.getController(force) shouldBe p2
            d.isTapped(force) shouldBe true
        }
    }
})
