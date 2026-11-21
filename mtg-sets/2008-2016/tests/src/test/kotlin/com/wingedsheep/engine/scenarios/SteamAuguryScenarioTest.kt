package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.SteamAugury
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Steam Augury: reveal the top five cards, *you* separate them into two piles, an *opponent*
 * chooses the pile that goes to your hand; the other goes to your graveyard.
 *
 * Pins the roles reversed from Fact or Fiction: the caster makes the split, the opponent picks
 * the pile — and exactly five cards are revealed.
 */
class SteamAuguryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SteamAugury)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("caster separates the top five, opponent picks the pile for the caster's hand") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        val c5 = driver.putCardOnTopOfLibrary(active, "Swamp")
        val c6 = driver.putCardOnTopOfLibrary(active, "Island")
        // Library from top: c6, c5, c4, c3, c2, c1 — the top five are c6..c2; c1 stays.

        val spell = driver.putCardInHand(active, "Steam Augury")
        driver.giveMana(active, Color.BLUE, 2)
        driver.giveMana(active, Color.RED, 2)
        val handBefore = driver.getHandSize(active)

        driver.castSpell(active, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        // 1. The caster separates the five revealed cards.
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        split.options.toSet() shouldBe setOf(c6, c5, c4, c3, c2)
        driver.submitDecision(active, CardsSelectedResponse(split.id, listOf(c6, c5)))

        // 2. The opponent chooses which pile goes to the caster's hand: Pile 1 (c6, c5).
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        val pile1 = choose.optionCardIds!!.entries.first { it.value.toSet() == setOf(c6, c5) }.key
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, pile1))

        driver.isPaused shouldBe false
        driver.state.getZone(ZoneKey(active, Zone.HAND)) shouldContainAll listOf(c6, c5)
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)) shouldContainAll listOf(c4, c3, c2, spell)
        driver.getHandSize(active) shouldBe handBefore - 1 + 2
        driver.state.getZone(ZoneKey(active, Zone.LIBRARY)).first() shouldBe c1
    }
})
