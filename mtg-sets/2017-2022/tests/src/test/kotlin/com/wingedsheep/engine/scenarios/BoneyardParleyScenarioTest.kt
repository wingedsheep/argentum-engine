package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.BoneyardParley
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Boneyard Parley: exile up to five target creature cards from graveyards, an opponent splits them
 * into two piles, you put the pile of your choice onto the battlefield under your control and the
 * rest into their owners' graveyards.
 *
 * Pins who decides what (opponent separates, you choose), that a creature card stolen from the
 * opponent's graveyard enters under your control, and that the other pile goes back to each
 * card's *owner's* graveyard rather than yours.
 */
class BoneyardParleyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BoneyardParley)
        return driver
    }

    fun GameTestDriver.zone(player: EntityId, zone: Zone) = state.getZone(ZoneKey(player, zone))

    fun GameTestDriver.castParley(active: EntityId, targets: List<Pair<EntityId, EntityId>>) {
        val spell = putCardInHand(active, "Boneyard Parley")
        giveMana(active, Color.BLACK, 2)
        giveColorlessMana(active, 5)
        castSpellWithTargets(
            active,
            spell,
            targets.map { (card, owner) -> ChosenTarget.Card(card, owner, Zone.GRAVEYARD) }
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("opponent separates, you take the pile with their creature, the rest returns to its owner's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCreature = driver.putCardInGraveyard(active, "Grizzly Bears")
        val theirCreature = driver.putCardInGraveyard(opponent, "Centaur Courser")

        driver.castParley(active, listOf(myCreature to active, theirCreature to opponent))

        // Both targets are exiled before the split.
        driver.zone(active, Zone.GRAVEYARD).contains(myCreature) shouldBe false
        driver.zone(opponent, Zone.GRAVEYARD).contains(theirCreature) shouldBe false

        // The opponent separates the cards: their own creature becomes Pile 1.
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe opponent
        driver.submitCardSelection(opponent, listOf(theirCreature))

        // You choose the pile that enters the battlefield.
        val pick = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe active
        pick.optionCardIds?.get(0)?.toSet() shouldBe setOf(theirCreature)
        pick.optionCardIds?.get(1)?.toSet() shouldBe setOf(myCreature)
        driver.submitDecision(active, OptionChosenResponse(pick.id, 0))
        driver.isPaused shouldBe false

        // The opponent's creature enters under your control.
        driver.zone(active, Zone.BATTLEFIELD).contains(theirCreature) shouldBe true
        driver.getController(theirCreature) shouldBe active
        // The other pile goes to its owner's graveyard.
        driver.zone(active, Zone.GRAVEYARD).contains(myCreature) shouldBe true
        driver.zone(active, Zone.EXILE).isEmpty() shouldBe true
        driver.zone(opponent, Zone.EXILE).isEmpty() shouldBe true
    }

    test("an empty pile lets you put every exiled card onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCardInGraveyard(active, "Grizzly Bears")
        val second = driver.putCardInGraveyard(opponent, "Centaur Courser")

        driver.castParley(active, listOf(first to active, second to opponent))

        // The opponent puts nothing in Pile 1, so Pile 2 holds both cards.
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(opponent, emptyList())

        val pick = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(active, OptionChosenResponse(pick.id, 1))
        driver.isPaused shouldBe false

        driver.getController(first) shouldBe active
        driver.getController(second) shouldBe active
        driver.zone(active, Zone.BATTLEFIELD).containsAll(listOf(first, second)) shouldBe true
    }
})
