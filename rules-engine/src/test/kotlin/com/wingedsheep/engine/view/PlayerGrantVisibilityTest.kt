package com.wingedsheep.engine.view

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantShroudToController
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Player-level shroud and hexproof have to show up as badges on the player, whether they came from
 * a resolution-time effect (Gilded Light, Dawn's Truce) or from a permanent that grants them
 * (True Believer, Shalai).
 *
 * Shroud only ever had the first half: the transformer checked `PlayerShroudComponent` and nothing
 * else, so a True Believer's controller was untargetable but shown as plain. That is the badge
 * that matters most — shroud stops the player's *own* spells too (CR 702.18), so without it the
 * client offers a self-target the server then rejects.
 *
 * Both now read through `ControllerShroud` / `ControllerHexproof`, which union the two sources and
 * re-evaluate any "as long as …" gate on every read, so the badge tracks the gate rather than
 * freezing at whatever it said when the permanent entered.
 */
class PlayerGrantVisibilityTest : FunSpec({

    val trueBeliever = card("Test True Believer") {
        manaCost = "{W}{W}"
        typeLine = "Creature — Human Cleric"
        oracleText = "You have shroud."
        power = 2
        toughness = 2
        staticAbility { ability = GrantShroudToController }
    }

    val shalai = card("Test Angel of Plenty") {
        manaCost = "{3}{W}"
        typeLine = "Creature — Angel"
        oracleText = "You have hexproof."
        power = 3
        toughness = 4
        staticAbility { ability = GrantHexproofToController }
    }

    // The gated form, as Captain America, Super-Soldier writes it.
    val gatedBeliever = card("Test Conditional Believer") {
        manaCost = "{W}{W}"
        typeLine = "Creature — Human Cleric"
        oracleText = "As long as you control two or more creatures, you have shroud."
        power = 2
        toughness = 2
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantShroudToController,
                condition = Conditions.YouControlAtLeast(2, Filters.Creature),
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + trueBeliever + shalai + gatedBeliever)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun badges(driver: GameTestDriver, playerId: EntityId): List<String> =
        ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = playerId)
            .players.single { it.playerId == playerId }
            .activeEffects.map { it.effectId }

    test("a permanent granting shroud badges its controller") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        withClue("no grant yet, so no badge") {
            badges(driver, player).contains("player_shroud") shouldBe false
        }

        driver.putCreatureOnBattlefield(player, "Test True Believer")

        withClue("True Believer's controller has shroud and must be told so") {
            badges(driver, player).contains("player_shroud") shouldBe true
        }
        withClue("the opponent gains nothing from it") {
            badges(driver, opponent).contains("player_shroud") shouldBe false
        }
    }

    test("a permanent granting hexproof badges its controller") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        badges(driver, player).contains("player_hexproof") shouldBe false
        driver.putCreatureOnBattlefield(player, "Test Angel of Plenty")
        badges(driver, player).contains("player_hexproof") shouldBe true
    }

    test("the badge leaves with the permanent") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val believer = driver.putCreatureOnBattlefield(player, "Test True Believer")
        badges(driver, player).contains("player_shroud") shouldBe true

        driver.moveToGraveyard(believer)
        withClue("the marker leaves with the permanent, so the badge must too") {
            badges(driver, player).contains("player_shroud") shouldBe false
        }
    }

    test("a gated grant badges only while its gate holds") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Test Conditional Believer")
        withClue("one creature, so the gate is open and the badge must be absent") {
            badges(driver, player).contains("player_shroud") shouldBe false
        }

        driver.putCreatureOnBattlefield(player, "Savannah Lions")
        withClue("the gate closed, so the badge must appear without the granter re-entering") {
            badges(driver, player).contains("player_shroud") shouldBe true
        }
    }

    test("two granting permanents still produce one badge") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Test True Believer")
        driver.putCreatureOnBattlefield(player, "Test True Believer")

        badges(driver, player).count { it == "player_shroud" } shouldBe 1
    }
})
