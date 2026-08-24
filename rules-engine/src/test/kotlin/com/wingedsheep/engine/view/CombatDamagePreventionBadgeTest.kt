package com.wingedsheep.engine.view

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A board-wide combat-damage shield (Fog, Holy Day, Spore Flower's ability) has to be visible to
 * both players once it resolves.
 *
 * It used to be visible to neither. The badge for it lived only in the card-level builder, which
 * skips any floating effect that doesn't name the card in `affectedEntities` — and these shields
 * name nothing at all: they're undirected, so `PreventDamageExecutor` stores an empty affected set.
 * The branch could therefore never fire, and a resolved Fog left the board looking untouched. The
 * player who spent three spore counters on Spore Flower had only the log line to go on, and the
 * attacking player had nothing to warn them their damage was about to evaporate.
 */
class CombatDamagePreventionBadgeTest : FunSpec({

    val testFog = card("Test Fog") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Prevent all combat damage that would be dealt this turn."
        spell {
            effect = Effects.PreventAllCombatDamage()
        }
    }

    // Deep Wood's shape: the same prevention, but scoped to the shield's controller.
    val testDeepWood = card("Test Deep Wood") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Prevent all damage that would be dealt to you this turn by attacking creatures."
        spell {
            effect = Effects.PreventDamageFromAttackingCreatures()
        }
    }

    // Frontline Strategist's shape: undirected like Fog, but narrowed to a group of sources.
    val testStrategist = card("Test Strategist's Order") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Prevent all combat damage non-Soldier creatures would deal this turn."
        spell {
            effect = Effects.PreventCombatDamageFrom(
                source = Filters.Group.creatures { notSubtype(Subtype("Soldier")) }
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + testFog + testDeepWood + testStrategist)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun badges(driver: GameTestDriver, playerId: EntityId): List<String> =
        ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = playerId)
            .players.single { it.playerId == playerId }
            .activeEffects.map { it.effectId }

    fun resolve(driver: GameTestDriver, playerId: EntityId, cardName: String, color: Color) {
        val cardId = driver.putCardInHand(playerId, cardName)
        driver.giveMana(playerId, color, 1)
        driver.castSpell(playerId, cardId)
        driver.bothPass()
    }

    test("a resolved Fog badges both players") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        withClue("nothing has resolved yet") {
            badges(driver, player).contains("prevent_all_combat_damage") shouldBe false
        }

        resolve(driver, player, "Test Fog", Color.GREEN)

        withClue("the player who cast it must see that their spell did something") {
            badges(driver, player).contains("prevent_all_combat_damage") shouldBe true
        }
        withClue("the shield stops every creature's combat damage, including the opponent's") {
            badges(driver, opponent).contains("prevent_all_combat_damage") shouldBe true
        }
    }

    test("two Fogs still produce one badge") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        resolve(driver, player, "Test Fog", Color.GREEN)
        resolve(driver, player, "Test Fog", Color.GREEN)

        badges(driver, player).count { it == "prevent_all_combat_damage" } shouldBe 1
    }

    test("a controller-scoped attacker shield badges only its controller") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        resolve(driver, player, "Test Deep Wood", Color.GREEN)

        badges(driver, player).contains("prevent_damage_from_attackers") shouldBe true
        withClue("the opponent is not the protected player") {
            badges(driver, opponent).contains("prevent_damage_from_attackers") shouldBe false
        }
    }

    test("a source-filtered shield names the sources it stops") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        resolve(driver, player, "Test Strategist's Order", Color.WHITE)

        val descriptions = ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = player)
            .players.single { it.playerId == player }
            .activeEffects
            .filter { it.effectId.startsWith("prevent_combat_damage_from_") }
            .mapNotNull { it.description }

        withClue("a bare \"No Combat Damage\" would overstate a filtered shield: $descriptions") {
            descriptions.size shouldBe 1
            descriptions.single().contains("Soldier") shouldBe true
        }
    }
})
