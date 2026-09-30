package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * [CantEnterTheBattlefield] is enforced at the zone-transition chokepoint, so every effect-driven
 * entry honours it — a lone move, a batch move — and a card that is locked out stays where it was
 * (CR 101.2: the "can't" wins; CR 304.4 is the same "remains in its previous zone" outcome). The
 * zone axis scopes it: a graveyard lock says nothing about a library or exile.
 */
class CantEnterTheBattlefieldTest : FunSpec({

    val graveyardLock = card("Graveyard Lock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Permanent cards in graveyards can't enter the battlefield."
        staticAbility {
            ability = CantEnterTheBattlefield(GameObjectFilter.Permanent, fromZones = setOf(Zone.GRAVEYARD))
        }
    }

    val creatureLock = card("Creature Lock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Creature cards can't enter the battlefield."
        staticAbility { ability = CantEnterTheBattlefield(GameObjectFilter.Creature) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(graveyardLock, creatureLock))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.move(ids: List<com.wingedsheep.sdk.model.EntityId>) {
        val result = zones.moveToZoneBatch(state, ids, Zone.BATTLEFIELD)
        replaceState(result.state)
    }

    test("a batch move out of every graveyard leaves the locked cards where they were, with no zone-change event") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Lock")
        val mine = driver.putCardInGraveyard(driver.player1, "Centaur Courser")
        val theirs = driver.putCardInGraveyard(driver.player2, "Forest")

        val result = driver.zones.moveToZoneBatch(driver.state, listOf(mine, theirs), Zone.BATTLEFIELD)
        driver.replaceState(result.state)

        result.events shouldBe emptyList()
        driver.getGraveyard(driver.player1) shouldContain mine
        driver.getGraveyard(driver.player2) shouldContain theirs
    }

    test("the zone axis scopes the lock: a graveyard lock lets a card in from exile") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Graveyard Lock")
        val exiled = driver.putCardInExile(driver.player1, "Centaur Courser")

        driver.move(listOf(exiled))

        driver.findPermanent(driver.player1, "Centaur Courser") shouldBe exiled
    }

    test("the filter scopes the lock: an unscoped creature lock stops a creature from anywhere but not a land") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Creature Lock")
        val exiled = driver.putCardInExile(driver.player1, "Centaur Courser")
        val land = driver.putCardInGraveyard(driver.player1, "Forest")

        driver.move(listOf(exiled, land))

        driver.getExile(driver.player1) shouldContainExactly listOf(exiled)
        driver.findPermanent(driver.player1, "Forest") shouldBe land
    }
})
