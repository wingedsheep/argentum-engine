package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The world rule (CR 704.5k): if two or more permanents have the supertype world, all except the
 * one that has had the world supertype for the shortest amount of time go to their owners'
 * graveyards; on a tie for the shortest time, all of them do. No player chooses, and unlike the
 * legend rule it compares permanents across every player's battlefield.
 */
class WorldRuleTest : FunSpec({

    val worldA = card("Test World A") {
        manaCost = "{G}"
        typeLine = "World Enchantment"
    }
    val worldB = card("Test World B") {
        manaCost = "{G}"
        typeLine = "World Enchantment"
    }
    val plainEnchantment = card("Test Plain Enchantment") {
        manaCost = "{G}"
        typeLine = "Enchantment"
    }

    val returnAllEnchantments = card("Test Return All Enchantments") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val returning = gather(
                    CardSource.FromZone(zone = Zone.GRAVEYARD, player = Player.Each, filter = GameObjectFilter.Enchantment)
                )
                move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            }
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(worldA, worldB, plainEnchantment, returnAllEnchantments))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castAndResolve(cardName: String): com.wingedsheep.sdk.model.EntityId {
        giveMana(player1, Color.GREEN, 1)
        val cardId = putCardInHand(player1, cardName)
        castSpell(player1, cardId)
        bothPass()
        return cardId
    }

    test("a lone world permanent stays, and non-world enchantments are untouched") {
        val driver = newDriver()
        val plain = driver.putPermanentOnBattlefield(driver.player1, "Test Plain Enchantment")
        val world = driver.castAndResolve("Test World A")

        driver.getPermanents(driver.player1) shouldContain world
        driver.getPermanents(driver.player1) shouldContain plain
    }

    test("a newer world permanent puts the older one into the graveyard, with no choice") {
        val driver = newDriver()
        val older = driver.castAndResolve("Test World A")
        val newer = driver.castAndResolve("Test World B")

        driver.pendingDecision shouldBe null
        driver.getPermanents(driver.player1) shouldContain newer
        driver.getPermanents(driver.player1) shouldNotContain older
        driver.getGraveyard(driver.player1) shouldContain older
    }

    test("a second copy of the same world permanent replaces the first") {
        val driver = newDriver()
        val first = driver.castAndResolve("Test World A")
        val second = driver.castAndResolve("Test World A")

        driver.getPermanents(driver.player1) shouldContain second
        driver.getGraveyard(driver.player1) shouldContain first
    }

    test("the rule spans players: an opponent's older world goes to its owner's graveyard") {
        val driver = newDriver()
        val opponentsWorld = driver.putPermanentOnBattlefield(driver.player2, "Test World A")
        val mine = driver.castAndResolve("Test World B")

        driver.getPermanents(driver.player1) shouldContain mine
        driver.getPermanents(driver.player2) shouldNotContain opponentsWorld
        driver.getGraveyard(driver.player2) shouldContain opponentsWorld
        driver.getGraveyard(driver.player1) shouldNotContain opponentsWorld
    }

    test("a tie for the shortest time puts every world permanent into the graveyard") {
        val driver = newDriver()
        val older = driver.castAndResolve("Test World A")
        val returnedA = driver.putCardInGraveyard(driver.player1, "Test World A")
        val returnedB = driver.putCardInGraveyard(driver.player2, "Test World B")

        // Both return in one simultaneous event: they tie for the shortest time, and the
        // world that was already on the battlefield is older than both.
        driver.castAndResolve("Test Return All Enchantments")

        driver.getGraveyard(driver.player1) shouldContain older
        driver.getGraveyard(driver.player1) shouldContain returnedA
        driver.getGraveyard(driver.player2) shouldContain returnedB
        (driver.getPermanents(driver.player1) + driver.getPermanents(driver.player2))
            .filter { driver.getCardName(it)?.startsWith("Test World") == true } shouldBe emptyList()
    }

    test("a world entering alone after a simultaneous group is the newest and survives") {
        val driver = newDriver()
        driver.putCardInGraveyard(driver.player1, "Test World A")
        driver.putCardInGraveyard(driver.player2, "Test World B")
        driver.castAndResolve("Test Return All Enchantments")
        val newest = driver.castAndResolve("Test World A")

        driver.getPermanents(driver.player1) shouldContain newest
        (driver.getPermanents(driver.player1) + driver.getPermanents(driver.player2))
            .filter { driver.getCardName(it)?.startsWith("Test World") == true } shouldBe listOf(newest)
    }
})
