package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.MustAttack
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MustAttack(playersOnly = true)` — "attacks a player each combat if able". The printed form is
 * projected (`mustAttackPlayer`), and the declaration validator rejects both not attacking and
 * attacking a planeswalker while the opponent player is a legal defender.
 */
class MustAttackAPlayerTest : FunSpec({

    val hunter = card("Player Hunter") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Warrior"
        power = 2
        toughness = 2
        oracleText = "This creature attacks a player each combat if able."
        staticAbility { ability = MustAttack(playersOnly = true) }
    }

    val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(hunter, walker))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the printed static projects both must-attack flags") {
        val driver = newDriver()
        val id = driver.putCreatureOnBattlefield(driver.player1, "Player Hunter")
        driver.state.projectedState.mustAttack(id) shouldBe true
        driver.state.projectedState.mustAttackPlayer(id) shouldBe true
    }

    test("attacking a planeswalker or not attacking is rejected; attacking the player is accepted") {
        val driver = newDriver()
        val id = driver.putCreatureOnBattlefield(driver.player1, "Player Hunter")
        driver.removeSummoningSickness(id)
        val pw = driver.putPermanentOnBattlefield(driver.player2, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
        )
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(id to pw)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(id to driver.player2)).error shouldBe null
    }
})
