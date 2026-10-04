package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.RootingMoloch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rooting Moloch {4}{R} — "When this creature enters, exile target card with a cycling ability from
 * your graveyard. Until the end of your next turn, you may play that card."
 *
 * Proves the `withCycling()` filter (plain cycling and typecycling both count, CR 702.29e; a card
 * without cycling doesn't) and that the exiled card is castable from exile.
 */
class RootingMolochScenarioTest : FunSpec({

    val cycler = card("Test Cycler") {
        manaCost = "{R}"
        typeLine = "Creature — Lizard"
        power = 2
        toughness = 1
        keywordAbility(KeywordAbility.cycling("{2}"))
    }

    val landcycler = card("Test Landcycler") {
        manaCost = "{5}{R}"
        typeLine = "Creature — Beast"
        power = 5
        toughness = 5
        keywordAbility(KeywordAbility.typecycling("Mountain", "{2}"))
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(cycler)
        driver.registerCard(landcycler)
        driver.registerCard(RootingMoloch)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("targets only graveyard cards with a cycling ability, typecycling included") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val plain = driver.putCardInGraveyard(me, "Test Cycler")
        val typed = driver.putCardInGraveyard(me, "Test Landcycler")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val oppCycler = driver.putCardInGraveyard(opp, "Test Cycler")

        val moloch = driver.putCardInHand(me, "Rooting Moloch")
        driver.giveMana(me, Color.RED, 5)
        driver.castSpell(me, moloch).outcome shouldBe Outcome.Done
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].shouldNotBeNull()
        legal shouldContainExactlyInAnyOrder listOf(plain, typed)
        legal shouldNotContain bears
        legal shouldNotContain oppCycler
    }

    test("exiles the chosen card and lets you cast it from exile") {
        val driver = setup()
        val me = driver.activePlayer!!

        val plain = driver.putCardInGraveyard(me, "Test Cycler")
        driver.putCardInGraveyard(me, "Test Landcycler")

        val moloch = driver.putCardInHand(me, "Rooting Moloch")
        driver.giveMana(me, Color.RED, 5)
        driver.castSpell(me, moloch).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.submitTargetSelection(me, listOf(plain))
        driver.bothPass()

        driver.getExile(me) shouldContain plain
        driver.getGraveyardCardNames(me) shouldContain "Test Landcycler"

        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, plain).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(me, "Test Cycler").shouldNotBeNull()
    }
})
