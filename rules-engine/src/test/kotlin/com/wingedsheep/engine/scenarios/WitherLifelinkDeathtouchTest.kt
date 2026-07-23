package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wither (CR 702.80) only changes the *form* in which damage is dealt — as -1/-1 counters
 * instead of being marked against toughness. It does not change the fact that damage is being
 * dealt, so damage-associated keywords still apply:
 *  - Lifelink (CR 702.15b): the source's controller still gains that much life.
 *  - Deathtouch (CR 702.2b): any nonzero amount is lethal, destroying the creature as an SBA
 *    (CR 704.5h) even though only a few -1/-1 counters were placed and toughness stays > 0.
 *
 * Repro of the reported bug: a creature given wither (Barbed Bloodletter) plus deathtouch and
 * lifelink (Scarblade's Malice) dealt its wither damage but neither gained life nor killed the
 * blocker via deathtouch.
 */
class WitherLifelinkDeathtouchTest : FunSpec({

    // 1 power so a single -1/-1 counter is not lethal by itself; high toughness so it survives
    // the block and we can attribute the blocker's death purely to deathtouch.
    val WitherStinger = CardDefinition.creature(
        name = "Wither Stinger",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Insect")),
        power = 1,
        toughness = 5,
        oracleText = "Wither, deathtouch, lifelink",
        keywords = setOf(Keyword.WITHER, Keyword.DEATHTOUCH, Keyword.LIFELINK)
    )

    val WitherOnly = CardDefinition.creature(
        name = "Wither Only",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Insect")),
        power = 1,
        toughness = 5,
        oracleText = "Wither",
        keywords = setOf(Keyword.WITHER)
    )

    val BigBlocker = CardDefinition.creature(
        name = "Big Blocker",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 4,
        toughness = 4,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WitherStinger, WitherOnly, BigBlocker))
        return driver
    }

    /** Drive a full block: [attacker] attacks, [blocker] blocks it, damage is dealt. */
    fun blockCombat(driver: GameTestDriver, attacker: EntityId, blocker: EntityId, defender: EntityId, attackerOwner: EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attackerOwner, listOf(attacker), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(blocker to listOf(attacker)))
        driver.bothPass()
        // First strike step (none), then combat damage step
        driver.bothPass()
        driver.bothPass()
    }

    test("wither + deathtouch destroys the blocker even though only one -1/-1 counter is placed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attackerOwner = driver.activePlayer!!
        val defender = driver.getOpponent(attackerOwner)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stinger = driver.putCreatureOnBattlefield(attackerOwner, "Wither Stinger")
        driver.removeSummoningSickness(stinger)
        val blocker = driver.putCreatureOnBattlefield(defender, "Big Blocker")
        driver.removeSummoningSickness(blocker)

        blockCombat(driver, stinger, blocker, defender, attackerOwner)

        // Deathtouch made the 1 point of wither damage lethal -> blocker destroyed.
        driver.assertInGraveyard(defender, "Big Blocker")
        // The wither attacker survived (toughness 5 vs 4 marked damage).
        driver.assertPermanentExists(attackerOwner, "Wither Stinger")
    }

    test("wither + lifelink gains life equal to the wither damage dealt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attackerOwner = driver.activePlayer!!
        val defender = driver.getOpponent(attackerOwner)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stinger = driver.putCreatureOnBattlefield(attackerOwner, "Wither Stinger")
        driver.removeSummoningSickness(stinger)
        val blocker = driver.putCreatureOnBattlefield(defender, "Big Blocker")
        driver.removeSummoningSickness(blocker)

        blockCombat(driver, stinger, blocker, defender, attackerOwner)

        // Lifelink: controller gains 1 life for the 1 wither damage dealt.
        driver.assertLifeTotal(attackerOwner, 21)
    }

    test("plain wither (no deathtouch) does not destroy a creature whose toughness stays above zero") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)

        val attackerOwner = driver.activePlayer!!
        val defender = driver.getOpponent(attackerOwner)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stinger = driver.putCreatureOnBattlefield(attackerOwner, "Wither Only")
        driver.removeSummoningSickness(stinger)
        val blocker = driver.putCreatureOnBattlefield(defender, "Big Blocker")
        driver.removeSummoningSickness(blocker)

        blockCombat(driver, stinger, blocker, defender, attackerOwner)

        // One -1/-1 counter, 4/4 -> 3/3, survives; no deathtouch, no life gained.
        driver.assertPermanentExists(defender, "Big Blocker")
        val counters = driver.state.getEntity(blocker)?.get<CountersComponent>()
        counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
        driver.assertLifeTotal(attackerOwner, 20)
    }
})
