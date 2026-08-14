package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.CuratorOfDestinies
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curator of Destinies: on ETB you look at your top five and split them into a face-down and a
 * face-up pile; an opponent chooses a pile; that pile goes to your hand, the other to your
 * graveyard.
 *
 * The mirror image of Sauron's Ransom — here *you* look and split and the *opponent* chooses — so
 * these tests pin the reverse visibility: the opponent never sees the whole five, only the pile you
 * turned face up. Visibility is expressed through [RevealedToComponent], the engine's record of what
 * a player may see in a hidden zone.
 */
class CuratorOfDestiniesScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CuratorOfDestinies)
        return driver
    }

    fun GameTestDriver.revealedTo(card: EntityId, player: EntityId): Boolean =
        state.getEntity(card)?.get<RevealedToComponent>()?.isRevealedTo(player) == true

    /** Empty a player's library, so the "look at the top five" gather comes up with nothing. */
    fun GameTestDriver.emptyLibrary(playerId: EntityId) {
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        var newState = state
        for (cardId in state.getZone(libraryZone)) {
            newState = newState.removeFromZone(libraryZone, cardId).withoutEntity(cardId)
        }
        replaceState(newState)
    }

    /**
     * Casts Curator from hand with mana granted, resolves it onto the battlefield, then resolves
     * the ETB trigger it put on the stack.
     */
    fun GameTestDriver.castCurator(active: EntityId) {
        val spell = putCardInHand(active, "Curator of Destinies")
        giveMana(active, Color.BLUE, 2)
        giveColorlessMana(active, 4)
        castSpell(active, spell).isSuccess shouldBe true
        bothPass() // Curator resolves; its ETB trigger goes on the stack
        bothPass() // the ETB trigger resolves
    }

    test("you look and split; only the face-up pile is shown to the opponent; the opponent's pick goes to your hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack a known top five (top first as pushed last).
        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        val c5 = driver.putCardOnTopOfLibrary(active, "Swamp")
        // Library from top: c5, c4, c3, c2, c1, <deck...>

        driver.castCurator(active)

        val handAfterCast = driver.getHandSize(active)
        val graveBefore = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)).size

        // 1. You — the controller — look at the top five and separate them. The look is private:
        //    the opponent has been shown none of the five.
        driver.isPaused shouldBe true
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        split.options.size shouldBe 5
        listOf(c1, c2, c3, c4, c5).forEach { driver.revealedTo(it, active) shouldBe true }
        listOf(c1, c2, c3, c4, c5).forEach { driver.revealedTo(it, opponent) shouldBe false }

        // Put c5 and c4 face up; c3, c2 and c1 stay face down.
        driver.submitDecision(active, CardsSelectedResponse(split.id, listOf(c5, c4)))

        // 2. An opponent chooses a pile. Now — and only now — the face-up pile is visible to them;
        //    the face-down pile is not.
        driver.isPaused shouldBe true
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        driver.revealedTo(c5, opponent) shouldBe true
        driver.revealedTo(c4, opponent) shouldBe true
        listOf(c1, c2, c3).forEach { driver.revealedTo(it, opponent) shouldBe false }
        choose.optionCardIds?.get(0) shouldBe listOf(c5, c4)
        choose.optionCardIds?.get(1) shouldBe listOf(c3, c2, c1)

        // The opponent picks the face-up pile → it goes to your hand, the rest to your graveyard.
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, 0))
        driver.isPaused shouldBe false

        val hand = driver.state.getZone(ZoneKey(active, Zone.HAND))
        hand.contains(c5) shouldBe true
        hand.contains(c4) shouldBe true
        driver.getHandSize(active) shouldBe handAfterCast + 2

        val grave = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD))
        listOf(c1, c2, c3).forEach { grave.contains(it) shouldBe true }
        grave.size shouldBe graveBefore + 3
    }

    test("the opponent may pick the face-down pile; a 5/0 split into an empty face-up pile is legal") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        val c5 = driver.putCardOnTopOfLibrary(active, "Swamp")
        val all = listOf(c1, c2, c3, c4, c5)

        driver.castCurator(active)
        val handAfterCast = driver.getHandSize(active)

        // Select nothing → an empty face-up pile and all five face down (a legal 5/0 split).
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(active, CardsSelectedResponse(split.id, emptyList()))

        // Nothing was turned face up, so the opponent sees none of the five when choosing.
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        all.forEach { driver.revealedTo(it, opponent) shouldBe false }

        // They pick the face-down pile of five → all five go to your hand, still unrevealed to them.
        val faceDownOption = choose.optionCardIds?.entries?.first { it.value.size == 5 }?.key ?: 1
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, faceDownOption))
        driver.isPaused shouldBe false

        val hand = driver.state.getZone(ZoneKey(active, Zone.HAND))
        all.forEach { hand.contains(it) shouldBe true }
        driver.getHandSize(active) shouldBe handAfterCast + 5
        all.forEach { driver.revealedTo(it, opponent) shouldBe false }
    }

    test("an empty library asks nobody to split or choose piles") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.emptyLibrary(active)

        val handBefore = driver.getHandSize(active)
        driver.castCurator(active)

        // The gather comes up empty, so the ability finishes without asking anyone anything, and
        // no cards move. The Curator itself still entered the battlefield.
        driver.isPaused shouldBe false
        driver.getHandSize(active) shouldBe handBefore
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)).size shouldBe 0
        driver.state.getZone(ZoneKey(active, Zone.BATTLEFIELD)).size shouldBe 1
    }

    test("Curator of Destinies can't be countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val curator = driver.putCardInHand(active, "Curator of Destinies")
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(active, Color.BLUE, 2)
        driver.giveColorlessMana(active, 4)
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.castSpell(active, curator).isSuccess shouldBe true
        driver.stackSize shouldBe 1
        driver.passPriority(active)

        // The opponent may still target the spell (per the 2024-11-08 ruling) …
        val curatorOnStack = driver.getTopOfStack()!!
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counter,
                targets = listOf(ChosenTarget.Spell(curatorOnStack)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.stackSize shouldBe 2

        // … but Counterspell resolves without countering it.
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.getTopOfStackName() shouldBe "Curator of Destinies"

        // Curator resolves and enters, so its ETB trigger asks the controller to split.
        driver.bothPass()
        driver.bothPass()
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        driver.findPermanent(active, "Curator of Destinies") shouldNotBe null
    }
})
