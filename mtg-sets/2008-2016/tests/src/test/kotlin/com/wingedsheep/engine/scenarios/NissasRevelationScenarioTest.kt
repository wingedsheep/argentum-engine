package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.NissasRevelation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nissa's Revelation — "Scry 5, then reveal the top card of your library. If it's a creature card,
 * you draw cards equal to its power and you gain life equal to its toughness."
 *
 * Pins that both amounts come from the revealed *library* card's own power and toughness (read
 * separately, never the battlefield), that a noncreature card does nothing, that power 0 draws
 * nothing while toughness still gains life, and that both values are locked in before the draw
 * moves the card — a hand-size characteristic-defining P/T gains the life it had when revealed
 * (CR 608.2h last-known information), not the doubled value it has once the cards are drawn.
 */
class NissasRevelationScenarioTest : FunSpec({
    val zeroPowerWall = card("Test Zero Power Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }
    val handAvatar = card("Test Hand Avatar") {
        manaCost = "{3}"
        typeLine = "Creature — Avatar"
        dynamicStats(DynamicAmounts.cardsInYourHand())
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(NissasRevelation, zeroPowerWall, handAvatar))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Puts [topCard] on top of player 1's library and casts Nissa's Revelation, pausing at the scry. */
    fun GameTestDriver.castWithTop(topCard: String): EntityId {
        val top = putCardOnTopOfLibrary(player1, topCard)
        val spell = putCardInHand(player1, "Nissa's Revelation")
        giveMana(player1, Color.GREEN, 7)
        castSpell(player1, spell).error shouldBe null
        bothPass().error shouldBe null
        return top
    }

    /** Scry 5: keep only [keep] on top, every other looked-at card goes to the bottom. */
    fun GameTestDriver.scryKeeping(keep: EntityId) {
        val scry = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe player1
        scry.options shouldHaveSize 5
        scry.options shouldContain keep
        submitCardSelection(player1, scry.options - keep).error shouldBe null
        val pending = pendingDecision
        if (pending is ReorderLibraryDecision) {
            submitDecision(player1, OrderedResponse(pending.id, listOf(keep))).error shouldBe null
        }
    }

    test("a revealed 3/1 creature draws three cards, the revealed card first, and gains 1 life") {
        val d = driver()
        val knight = d.castWithTop("First Strike Knight")
        val handBefore = d.getHandSize(d.player1)
        val lifeBefore = d.getLifeTotal(d.player1)

        d.scryKeeping(knight)
        d.pendingDecision shouldBe null

        d.getHandSize(d.player1) shouldBe handBefore + 3
        d.getHand(d.player1) shouldContain knight
        d.getLifeTotal(d.player1) shouldBe lifeBefore + 1
    }

    test("a revealed noncreature card draws nothing, gains nothing, and stays on top") {
        val d = driver()
        val bolt = d.castWithTop("Lightning Bolt")
        val handBefore = d.getHandSize(d.player1)
        val lifeBefore = d.getLifeTotal(d.player1)

        d.scryKeeping(bolt)
        d.pendingDecision shouldBe null

        d.getHandSize(d.player1) shouldBe handBefore
        d.getLifeTotal(d.player1) shouldBe lifeBefore
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe bolt
    }

    test("a revealed 0/4 creature draws nothing but still gains 4 life") {
        val d = driver()
        val wall = d.castWithTop("Test Zero Power Wall")
        val handBefore = d.getHandSize(d.player1)
        val lifeBefore = d.getLifeTotal(d.player1)

        d.scryKeeping(wall)
        d.pendingDecision shouldBe null

        d.getHandSize(d.player1) shouldBe handBefore
        d.getLifeTotal(d.player1) shouldBe lifeBefore + 4
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe wall
    }

    test("a hand-size characteristic-defining P/T is read as revealed, before the draw changes it") {
        val d = driver()
        val avatar = d.castWithTop("Test Hand Avatar")
        // The spell has left the hand, so the avatar's P/T in the library equals this hand size.
        val handAtReveal = d.getHandSize(d.player1)
        val lifeBefore = d.getLifeTotal(d.player1)

        d.scryKeeping(avatar)
        d.pendingDecision shouldBe null

        d.getHandSize(d.player1) shouldBe handAtReveal * 2
        d.getHand(d.player1) shouldContain avatar
        // Gains the toughness the avatar had when revealed, not the doubled hand size after drawing.
        d.getLifeTotal(d.player1) shouldBe lifeBefore + handAtReveal
    }
})
