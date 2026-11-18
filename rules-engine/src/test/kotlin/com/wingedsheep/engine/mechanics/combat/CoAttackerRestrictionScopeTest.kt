package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [CantAttackUnlessCoAttacker] / [CantBlockUnlessCoBlocker] read from any permanent whose filter
 * reaches the creature, not only from the creature itself: "Enchanted creature can't attack alone"
 * (Sightless Brawler bestowed) binds the host. Each restriction is checked against the whole
 * declaration (CR 508.1c / 509.1b); two creatures that can't attack alone may attack together.
 */
class CoAttackerRestrictionScopeTest : FunSpec({

    val shackle = card("Test Lonely Shackle") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nEnchanted creature can't attack alone and can't block alone."
        staticAbility {
            ability = CantAttackUnlessCoAttacker(GameObjectFilter.Creature, filter = GroupFilter.attachedCreature())
        }
        staticAbility {
            ability = CantBlockUnlessCoBlocker(GameObjectFilter.Creature, filter = GroupFilter.attachedCreature())
        }
    }

    val loner = card("Test Loner") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Cat"
        power = 2
        toughness = 2
        oracleText = "This creature can't attack alone."
        staticAbility { ability = CantAttackUnlessCoAttacker(GameObjectFilter.Creature) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(shackle, loner))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attach(aura: EntityId, host: EntityId) {
        replaceState(
            state.updateEntity(aura) { it.with(AttachedToComponent(host)) }
                .updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) }
        )
    }

    fun GameTestDriver.creature(player: EntityId, name: String) =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    test("the enchanted creature can't attack alone, but may with another creature") {
        val driver = newDriver()
        val host = driver.creature(driver.player1, "Centaur Courser")
        val other = driver.creature(driver.player1, "Savannah Lions")
        driver.attach(driver.putPermanentOnBattlefield(driver.player1, "Test Lonely Shackle"), host)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(host to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(host to driver.player2, other to driver.player2)).error shouldBe null
    }

    test("it binds only the enchanted creature — another creature of its controller attacks alone") {
        val driver = newDriver()
        val host = driver.creature(driver.player1, "Centaur Courser")
        val other = driver.creature(driver.player1, "Savannah Lions")
        driver.attach(driver.putPermanentOnBattlefield(driver.player1, "Test Lonely Shackle"), host)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(other to driver.player2)).error shouldBe null
    }

    test("an Aura its opponent controls still binds the enchanted creature") {
        val driver = newDriver()
        val host = driver.creature(driver.player1, "Centaur Courser")
        driver.creature(driver.player1, "Savannah Lions")
        driver.attach(driver.putPermanentOnBattlefield(driver.player2, "Test Lonely Shackle"), host)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(host to driver.player2)).error shouldNotBe null
    }

    test("two creatures that can't attack alone may attack together") {
        val driver = newDriver()
        val host = driver.creature(driver.player1, "Centaur Courser")
        val loner = driver.creature(driver.player1, "Test Loner")
        driver.attach(driver.putPermanentOnBattlefield(driver.player1, "Test Lonely Shackle"), host)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(driver.player1, mapOf(loner to driver.player2)).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(host to driver.player2, loner to driver.player2)).error shouldBe null
    }

    test("the enchanted creature can't block alone, but may with another blocker") {
        val driver = newDriver()
        val attacker = driver.creature(driver.player1, "Centaur Courser")
        val host = driver.creature(driver.player2, "Centaur Courser")
        val other = driver.creature(driver.player2, "Savannah Lions")
        driver.attach(driver.putPermanentOnBattlefield(driver.player2, "Test Lonely Shackle"), host)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, mapOf(attacker to driver.player2)).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(driver.player2, mapOf(host to listOf(attacker))).error shouldNotBe null
        driver.declareBlockers(driver.player2, mapOf(host to listOf(attacker), other to listOf(attacker))).error shouldBe null
    }
})
