package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A shuffle destroys what players know about library order, so any card the opponent had seen in
 * that library must stop being identifiable afterwards.
 *
 * "Shuffle your graveyard into your library" with an **empty** graveyard takes MoveCollectionExecutor's
 * empty-collection branch, which must clear the library's reveals before shuffling, like every other
 * shuffle path (#3341).
 */
class EmptyShuffleClearsRevealsScenarioTest : FunSpec({

    val graveyardShuffler = card("Test Graveyard Shuffler") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(graveyardShuffler)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("shuffling an empty graveyard into the library clears the opponent's knowledge of library cards") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.player2

        // The opponent has seen the top card of our library.
        val seenCard = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).first()
        driver.replaceState(
            driver.state.updateEntity(seenCard) { it.with(RevealedToComponent.to(opponent)) }
        )
        driver.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).size shouldBe 0
        val shufflesBefore = driver.events.count { it is LibraryShuffledEvent && it.playerId == you }

        val spell = driver.putCardInHand(you, "Test Graveyard Shuffler")
        driver.giveMana(you, Color.BLUE, 1)
        driver.submit(CastSpell(you, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("the library was shuffled") {
            driver.events.count { it is LibraryShuffledEvent && it.playerId == you } shouldBe shufflesBefore + 1
        }
        withClue("after a shuffle, the previously seen card must no longer be revealed to the opponent") {
            driver.state.getEntity(seenCard)?.get<RevealedToComponent>() shouldBe null
        }
    }
})
