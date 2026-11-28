package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDamageWithMill
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ReplaceDamageWithMill] in its "instead" form — `miller = Player.TriggeringPlayer` (the damaged
 * player mills) and `prevents = false` (not a prevention effect, so it also replaces damage that
 * can't be prevented). Undead Alchemist's shape, tested on noncombat damage so the general damage
 * path is covered alongside the card's combat scenario test.
 */
class ReplaceDamageWithMillInsteadTest : FunSpec({

    val replacement = ReplaceDamageWithMill(
        appliesTo = EventPattern.DamageEvent(recipient = Recipient.Any),
        miller = Player.TriggeringPlayer,
        prevents = false,
    )
    val host = card("Test Mill Instead") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(replacement)
    }
    val leyline = card("Test Unpreventable") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(DamageCantBePrevented())
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(host, leyline))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED)
        castSpell(player1, putCardInHand(player1, "Lightning Bolt"), listOf(target)).error shouldBe null
        bothPass().error shouldBe null
    }

    test("description reads as an instead effect on the damaged player") {
        replacement.description.endsWith(", instead that player mills that many cards") shouldBe true
    }

    test("damage to a player is replaced: no life lost, the damaged player mills that many, not the controller") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Test Mill Instead")
        val ownLibrary = d.state.getLibrary(d.player1).size
        d.bolt(d.player2)

        d.assertLifeTotal(d.player2, 20)
        d.getGraveyard(d.player2).size shouldBe 3
        d.state.getLibrary(d.player1).size shouldBe ownLibrary
    }

    test("not prevention: damage that can't be prevented is still replaced") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Test Mill Instead")
        d.putPermanentOnBattlefield(d.player1, "Test Unpreventable")
        d.bolt(d.player2)

        d.assertLifeTotal(d.player2, 20)
        d.getGraveyard(d.player2).size shouldBe 3
    }

    test("damage to a permanent has no damaged player to mill, so it is dealt normally") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Test Mill Instead")
        val bears = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        d.bolt(bears)

        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        d.getGraveyardCardNames(d.player2) shouldBe listOf("Grizzly Bears")
    }
})
