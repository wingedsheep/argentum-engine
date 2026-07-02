package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.LightningArmyOfOne
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lightning, Army of One — {1}{R}{W} 3/2 Legendary Creature — Human Soldier.
 * First strike, trample, lifelink.
 * "Stagger — Whenever Lightning deals combat damage to a player, until your next turn, if a source
 * would deal damage to that player or a permanent that player controls, it deals double that damage
 * instead."
 *
 * Damage doubling is supplied by Lightning Bolt (3 → doubled to 6), a source distinct from Lightning
 * (proving the doubling covers *any* source, not just Lightning).
 */
class LightningArmyOfOneScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LightningArmyOfOne))
        return driver
    }

    /** Attack the opponent with an unblocked Lightning so its Stagger trigger installs the doubling. */
    fun installStagger(driver: GameTestDriver, me: EntityId, opp: EntityId): EntityId {
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val lightning = driver.putCreatureOnBattlefield(me, "Lightning, Army of One")
        driver.removeSummoningSickness(lightning)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(lightning), defendingPlayer = opp).error shouldBe null
        // Auto-passes empty blockers, deals combat damage, then resolves the Stagger trigger.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        return lightning
    }

    fun staggerEffectsFor(driver: GameTestDriver, player: EntityId): Int =
        driver.state.floatingEffects.count {
            val mod = it.effect.modification
            mod is SerializableModification.DoubleDamageToPlayer && mod.playerId == player
        }

    test("Stagger installs a doubling scoped to the damaged player; combat damage itself is NOT doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        installStagger(driver, me, opp)

        // Combat damage (3) is dealt before the trigger installs the doubling, so it is not doubled.
        driver.getLifeTotal(opp) shouldBe 17
        // Lifelink gained me 3.
        driver.getLifeTotal(me) shouldBe 23
        // The doubling is now live and scoped to the opponent only.
        staggerEffectsFor(driver, opp) shouldBe 1
        staggerEffectsFor(driver, me) shouldBe 0
    }

    test("a later source's damage to that player is doubled (Bolt 3 -> 6)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        installStagger(driver, me, opp) // opp at 17, doubling live

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(opp)))
        driver.bothPass()

        // 17 - (3 doubled to 6) = 11.
        driver.getLifeTotal(opp) shouldBe 11
    }

    test("damage to a permanent that player controls is doubled") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        installStagger(driver, me, opp)

        // Force of Nature is a 5/5 the opponent controls. A Bolt deals 3 (would survive), but Stagger
        // doubles it to 6 — lethal. Its death proves the doubling covers the player's permanents.
        val forceOfNature = driver.putCreatureOnBattlefield(opp, "Force of Nature")
        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Permanent(forceOfNature)))
        driver.bothPass()

        driver.state.getBattlefield().contains(forceOfNature) shouldBe false
    }

    test("the doubling expires on your next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        installStagger(driver, me, opp)
        staggerEffectsFor(driver, opp) shouldBe 1

        // Advance through the opponent's turn to my next turn's upkeep — past my untap step, where
        // UntilYourNextTurn floating effects are expired.
        driver.passPriorityUntil(Step.UPKEEP) // opponent's upkeep (next turn)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN) // opponent's second main
        driver.passPriorityUntil(Step.UPKEEP) // my next upkeep (after my untap)

        driver.activePlayer shouldBe me
        staggerEffectsFor(driver, opp) shouldBe 0
    }
})
