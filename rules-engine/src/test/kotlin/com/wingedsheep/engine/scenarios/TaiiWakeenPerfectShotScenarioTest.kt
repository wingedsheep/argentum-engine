package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TaiiWakeenPerfectShot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Taii Wakeen, Perfect Shot — {R}{W} 2/3 Legendary Creature — Human Mercenary
 *
 * 1. "Whenever a source you control deals noncombat damage to a creature equal to that creature's
 *    toughness, draw a card."
 * 2. "{X}, {T}: If a source you control would deal noncombat damage to a permanent or player this
 *    turn, it deals that much damage plus X instead."
 *
 * Noncombat damage is supplied by Lightning Bolt (3 damage), a source the caster controls.
 */
class TaiiWakeenPerfectShotScenarioTest : FunSpec({

    val taiiAbilityId = TaiiWakeenPerfectShot.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TaiiWakeenPerfectShot))
        return driver
    }

    // =========================================================================
    // Ability 1 — draw on noncombat damage equal to the creature's toughness.
    // =========================================================================

    test("noncombat damage exactly equal to a creature's toughness draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Taii Wakeen, Perfect Shot")
        // Centaur Courser is a 3/3 controlled by the opponent.
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        val handBefore = driver.getHandSize(me)

        // Bolt (3 damage) to a 3-toughness creature — damage == toughness.
        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(courser)))
        driver.bothPass() // resolve Bolt -> deals 3 -> Taii's trigger goes on the stack
        driver.bothPass() // resolve Taii's trigger -> draw a card

        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("noncombat damage greater than the creature's toughness does NOT draw") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Taii Wakeen, Perfect Shot")
        // Savannah Lions is a 1/1: Bolt's 3 > 1 toughness — over-lethal, no draw.
        val lions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")

        val handBefore = driver.getHandSize(me)

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(lions)))
        driver.passPriorityUntil(Step.END)

        driver.getHandSize(me) shouldBe handBefore
    }

    test("noncombat damage to a player does NOT draw (recipient is not a creature)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Taii Wakeen, Perfect Shot")

        val handBefore = driver.getHandSize(me)

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(opp)))
        driver.passPriorityUntil(Step.END)

        driver.getLifeTotal(opp) shouldBe 17
        driver.getHandSize(me) shouldBe handBefore
    }

    // =========================================================================
    // Ability 2 — {X}, {T}: noncombat damage you'd deal this turn gets +X.
    // =========================================================================

    test("{X=2}, {T}: a later noncombat ping deals X more (Bolt 3 -> 5)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val taii = driver.putCreatureOnBattlefield(me, "Taii Wakeen, Perfect Shot")
        driver.removeSummoningSickness(taii)

        // Activate {X=2}, {T}.
        driver.giveColorlessMana(me, 2)
        val activation = driver.submit(
            ActivateAbility(playerId = me, sourceId = taii, abilityId = taiiAbilityId, xValue = 2)
        )
        activation.isSuccess shouldBe true
        driver.bothPass() // resolve the ability -> install the until-end-of-turn +2 amplification

        // Bolt the opponent: 3 base + 2 = 5 damage.
        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(opp)))
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 15
    }

    test("the +X amplification expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val taii = driver.putCreatureOnBattlefield(me, "Taii Wakeen, Perfect Shot")
        driver.removeSummoningSickness(taii)

        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(playerId = me, sourceId = taii, abilityId = taiiAbilityId, xValue = 2)
        )
        driver.bothPass() // resolve -> floating amplification installed

        // The amplification floating effect is live this turn.
        driver.state.floatingEffects.count {
            it.effect.modification is SerializableModification.AmplifyNoncombatDamage
        } shouldBe 1

        // Advance to the next turn — the until-end-of-turn effect is cleaned up.
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.floatingEffects.count {
            it.effect.modification is SerializableModification.AmplifyNoncombatDamage
        } shouldBe 0
    }
})
