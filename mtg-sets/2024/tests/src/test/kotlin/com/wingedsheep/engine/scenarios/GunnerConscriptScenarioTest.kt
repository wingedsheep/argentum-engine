package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.GunnerConscript
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gunner Conscript (PIP #77).
 *
 * - "This creature gets +1/+1 for each Aura and Equipment attached to it."
 * - "When this creature dies, if it was enchanted, create a Junk token."
 * - "When this creature dies, if it was equipped, create a Junk token."
 *
 * The dies triggers read last-known information: by the time they're checked the Aura has
 * gone to the graveyard and the Equipment has become unattached, so only the zone change's
 * `wasEnchanted` / `wasEquipped` snapshot can answer them.
 */
class GunnerConscriptScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {3}"
        equipAbility("{3}")
    }

    val testCharm = card("Test Charm") {
        manaCost = "{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GunnerConscript, PredefinedTokens.Junk, testBlade, testCharm))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attach(player: EntityId, host: EntityId, vararg names: String) {
        val attachments = names.map { name ->
            putPermanentOnBattlefield(player, name).also { addComponent(it, AttachedToComponent(host)) }
        }
        addComponent(host, AttachmentsComponent(attachments))
    }

    fun GameTestDriver.junk(player: EntityId) = getPermanents(player).filter { getCardName(it) == "Junk" }

    fun GameTestDriver.destroy(player: EntityId, creature: EntityId) {
        val blade = putCardInHand(player, "Doom Blade")
        giveMana(player, Color.BLACK, 2)
        castSpell(player, blade, listOf(creature)).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority(state.priorityPlayerId!!).error shouldBe null
        state.stack.size shouldBe 0
    }

    test("gets +1/+1 for each Aura and Equipment attached to it") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gunner = driver.putCreatureOnBattlefield(me, "Gunner Conscript")
        driver.state.projectedState.getPower(gunner) shouldBe 2
        driver.state.projectedState.getToughness(gunner) shouldBe 2

        driver.attach(me, gunner, "Test Charm", "Test Blade")
        driver.state.projectedState.getPower(gunner) shouldBe 4
        driver.state.projectedState.getToughness(gunner) shouldBe 4
    }

    test("dying while enchanted creates one Junk") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gunner = driver.putCreatureOnBattlefield(me, "Gunner Conscript")
        driver.attach(me, gunner, "Test Charm")

        driver.destroy(me, gunner)
        driver.junk(me).size shouldBe 1
    }

    test("dying while equipped creates one Junk") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gunner = driver.putCreatureOnBattlefield(me, "Gunner Conscript")
        driver.attach(me, gunner, "Test Blade")

        driver.destroy(me, gunner)
        driver.junk(me).size shouldBe 1
    }

    test("dying while both enchanted and equipped creates two Junk") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gunner = driver.putCreatureOnBattlefield(me, "Gunner Conscript")
        driver.attach(me, gunner, "Test Charm", "Test Blade")

        driver.destroy(me, gunner)
        driver.junk(me).size shouldBe 2
    }

    test("dying with nothing attached creates no Junk") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gunner = driver.putCreatureOnBattlefield(me, "Gunner Conscript")

        driver.destroy(me, gunner)
        driver.junk(me).size shouldBe 0
    }
})
