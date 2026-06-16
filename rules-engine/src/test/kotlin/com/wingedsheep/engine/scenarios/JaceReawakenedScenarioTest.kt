package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.JaceReawakened
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Jace Reawakened.
 *
 * Jace Reawakened ({U}{U}): Legendary Planeswalker — Jace, starting loyalty 3.
 * - Can't be cast during your first/second/third turns of the game.
 * - +1: Draw a card, then discard a card.
 * - +1: You may exile a nonland card with mana value 3 or less from your hand; it becomes plotted.
 * - −6: Until end of turn, whenever you cast a spell, copy it. You may choose new targets.
 */
class JaceReawakenedScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JaceReawakened))
        return driver
    }

    /** Force the active player's "turns taken this game" so the cast-restriction condition flips. */
    fun GameTestDriver.setTurnsTaken(playerId: com.wingedsheep.sdk.model.EntityId, count: Int) {
        replaceState(state.updateEntity(playerId) { c -> c.with(PlayerTurnsTakenComponent(count = count)) })
    }

    test("cannot be cast during the first three turns of the game") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player has taken their first turn — casting Jace must be illegal.
        driver.setTurnsTaken(player, 1)
        val jace = driver.putCardInHand(player, "Jace Reawakened")
        driver.giveMana(player, Color.BLUE, 2)

        val result = driver.submit(CastSpell(playerId = player, cardId = jace))
        result.error shouldNotBe null
        // Still in hand.
        (jace in driver.state.getZone(ZoneKey(player, Zone.HAND))) shouldBe true
    }

    test("can be cast once the controller has taken four or more turns") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.setTurnsTaken(player, 4)
        val jace = driver.putCardInHand(player, "Jace Reawakened")
        driver.giveMana(player, Color.BLUE, 2)

        val result = driver.submit(CastSpell(playerId = player, cardId = jace))
        result.error shouldBe null
        driver.bothPass() // resolve onto the battlefield

        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == "Jace Reawakened"
        } shouldBe true
    }

    test("+1 plot: exile a nonland MV<=3 card from hand and it becomes plotted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val jace = driver.putPermanentOnBattlefield(player, "Jace Reawakened")
        // An MV-3 nonland creature, eligible to plot (MV <= 3).
        val courser = driver.putCardInHand(player, "Centaur Courser")
        // A high-MV card that must NOT be eligible (MV 12).
        driver.putCardInHand(player, "Ghalta, Primal Hunger")

        val plotAbility = driver.cardRegistry.requireCard("Jace Reawakened")
            .script.activatedAbilities.first { it.description.contains("plotted", ignoreCase = true) }

        driver.submit(ActivateAbility(player, jace, plotAbility.id))
        driver.bothPass() // resolve the loyalty ability

        // Resolution pauses to let the controller choose the card to exile and plot.
        driver.submitCardSelection(player, listOf(courser))

        (courser in driver.state.getZone(ZoneKey(player, Zone.EXILE))) shouldBe true
        driver.state.getEntity(courser)?.get<PlottedComponent>() shouldNotBe null
    }

    test("+1 loot: draw a card, then discard a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val jace = driver.putPermanentOnBattlefield(player, "Jace Reawakened")
        val discardFodder = driver.putCardInHand(player, "Island")
        val handBefore = driver.state.getZone(ZoneKey(player, Zone.HAND)).size

        // The first loyalty ability (index 0) is the +1 "draw a card, then discard a card" loot.
        val lootAbility = driver.cardRegistry.requireCard("Jace Reawakened")
            .script.activatedAbilities[0]

        driver.submit(ActivateAbility(player, jace, lootAbility.id))
        driver.bothPass() // resolve: draw, then a discard selection pauses

        driver.submitCardSelection(player, listOf(discardFodder))

        // Net hand size: +1 draw, -1 discard = unchanged.
        driver.state.getZone(ZoneKey(player, Zone.HAND)).size shouldBe handBefore
        (discardFodder in driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD))) shouldBe true
    }
})
