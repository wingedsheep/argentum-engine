package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmount.PartySize` — the number of creatures in a player's party (CR 700.8): up to one
 * each of Cleric, Rogue, Warrior and Wizard, 0–4, computed automatically (CR 700.8a); a creature
 * with several party types fills one slot, counted the way that gives the highest result
 * (CR 700.8b); four is a full party (CR 700.8c, `Conditions.FullParty`).
 */
class PartySizeTest : FunSpec({

    fun creature(name: String, typeLine: String, changeling: Boolean = false) = card(name) {
        manaCost = "{1}"
        this.typeLine = typeLine
        power = 1
        toughness = 1
        if (changeling) keywords(Keyword.CHANGELING)
    }

    val cards = listOf(
        creature("Test Cleric", "Creature — Human Cleric"),
        creature("Test Rogue", "Creature — Human Rogue"),
        creature("Test Warrior", "Creature — Human Warrior"),
        creature("Test Wizard", "Creature — Human Wizard"),
        creature("Test Cleric Wizard", "Creature — Human Cleric Wizard"),
        creature("Test Cleric Rogue", "Creature — Human Cleric Rogue"),
        creature("Test Shapeshifter", "Creature — Shapeshifter", changeling = true),
        card("Test Wizard Totem") {
            manaCost = "{1}"
            typeLine = "Kindred Artifact — Wizard"
        },
    )

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + cards)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.party(player: EntityId) =
        services.predicateEvaluator.amounts.evaluate(
            state, DynamicAmounts.partySize(), EffectContext(sourceId = null, controllerId = player)
        )

    fun GameTestDriver.fullParty(player: EntityId) =
        services.predicateEvaluator.conditions.evaluate(
            state, Conditions.FullParty(), EffectContext(sourceId = null, controllerId = player)
        )

    fun GameTestDriver.put(player: EntityId, vararg names: String) =
        names.forEach { putCreatureOnBattlefield(player, it) }

    test("no party creatures is a party of zero") {
        val d = driver(); val me = d.activePlayer!!
        d.party(me) shouldBe 0
        d.put(me, "Grizzly Bears")
        d.party(me) shouldBe 0
        d.fullParty(me) shouldBe false
    }

    test("one of each type is a full party") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Cleric", "Test Rogue", "Test Warrior")
        d.party(me) shouldBe 3
        d.fullParty(me) shouldBe false
        d.put(me, "Test Wizard")
        d.party(me) shouldBe 4
        d.fullParty(me) shouldBe true
    }

    test("a second creature of a filled type doesn't grow the party") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Cleric", "Test Cleric", "Test Cleric")
        d.party(me) shouldBe 1
    }

    test("the party never exceeds four") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Cleric", "Test Rogue", "Test Warrior", "Test Wizard", "Test Cleric Wizard", "Test Shapeshifter")
        d.party(me) shouldBe 4
    }

    test("a multi-type creature fills only one slot") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Cleric Wizard")
        d.party(me) shouldBe 1
        d.put(me, "Test Wizard")
        d.party(me) shouldBe 2
    }

    test("multi-type creatures are assigned to give the highest count") {
        val d = driver(); val me = d.activePlayer!!
        // Counting the Cleric Rogue as the Cleric leaves the Cleric unplaced; as the Rogue, both fit.
        d.put(me, "Test Cleric Rogue", "Test Cleric")
        d.party(me) shouldBe 2
        d.put(me, "Test Cleric Wizard")
        d.party(me) shouldBe 3
    }

    test("a changeling fills whichever slot is still open") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Cleric", "Test Rogue", "Test Warrior", "Test Shapeshifter")
        d.party(me) shouldBe 4
    }

    test("changelings alone can make a full party") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, "Test Shapeshifter", "Test Shapeshifter", "Test Shapeshifter")
        d.party(me) shouldBe 3
        d.put(me, "Test Shapeshifter")
        d.fullParty(me) shouldBe true
    }

    test("only creatures count") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Test Wizard Totem")
        d.party(me) shouldBe 0
    }

    test("only the player's own creatures count") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(opp, "Test Cleric", "Test Rogue", "Test Warrior", "Test Wizard")
        d.party(me) shouldBe 0
        d.party(opp) shouldBe 4
        d.services.predicateEvaluator.amounts.evaluate(
            d.state, DynamicAmounts.partySize(Player.EachOpponent), EffectContext(sourceId = null, controllerId = me)
        ) shouldBe 4
    }
})
