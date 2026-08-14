package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.UnstableExperiment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unstable Experiment (SPM #47) — {1}{U} Instant.
 *   Target player draws a card, then up to one target creature you control connives.
 *
 * Two targets are chosen at cast time: the player who draws (can be an opponent) and — as an
 * "up to one target" — an optional creature you control that connives. These tests verify:
 *   - the target player draws, then your creature connives (nonland discard → +1/+1 counter);
 *   - discarding a land during connive adds no counter;
 *   - choosing no creature runs only the target player's draw — you neither draw nor discard.
 */
class UnstableExperimentScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UnstableExperiment)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    test("target player draws; the chosen creature connives and a nonland discard grows it") {
        val driver = createDriver()
        val caster = driver.player1
        val drawer = driver.player2

        val creature = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        // The opponent's "target player" draw pulls this card off their library.
        val opponentDraw = driver.putCardOnTopOfLibrary(drawer, "Island")
        // The connive draw pulls a land off the caster's library (kept), leaving the pre-placed
        // nonland in hand as the card we discard.
        driver.putCardOnTopOfLibrary(caster, "Island")
        val nonlandToDiscard = driver.putCardInHand(caster, "Grizzly Bears")

        val spell = driver.putCardInHand(caster, "Unstable Experiment")
        driver.giveMana(caster, Color.BLUE, 2)

        driver.castSpell(caster, spell, targets = listOf(drawer, creature)).isSuccess shouldBe true
        driver.bothPass()

        // The opponent has already drawn; resolution now pauses on the connive discard choice.
        driver.state.getHand(drawer).contains(opponentDraw) shouldBe true
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            caster,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(nonlandToDiscard))
        )
        driver.isPaused shouldBe false

        driver.state.getGraveyard(caster).contains(nonlandToDiscard) shouldBe true
        val counters = driver.state.getEntity(creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }

    test("discarding a land during connive adds no counter") {
        val driver = createDriver()
        val caster = driver.player1
        val drawer = driver.player2

        val creature = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        driver.putCardOnTopOfLibrary(drawer, "Island")
        driver.putCardOnTopOfLibrary(caster, "Grizzly Bears")
        val landToDiscard = driver.putCardInHand(caster, "Island")

        val spell = driver.putCardInHand(caster, "Unstable Experiment")
        driver.giveMana(caster, Color.BLUE, 2)

        driver.castSpell(caster, spell, targets = listOf(drawer, creature)).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            caster,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(landToDiscard))
        )
        driver.isPaused shouldBe false

        driver.state.getGraveyard(caster).contains(landToDiscard) shouldBe true
        val counters = driver.state.getEntity(creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }

    test("choosing no creature runs only the target player's draw — no connive draw or discard") {
        val driver = createDriver()
        val caster = driver.player1
        val drawer = driver.player2

        // A creature is on the battlefield, but the "up to one" target is left empty.
        val creature = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")

        val opponentDraw = driver.putCardOnTopOfLibrary(drawer, "Island")

        val spell = driver.putCardInHand(caster, "Unstable Experiment")
        val casterHandBefore = driver.getHandSize(caster) // just the spell
        driver.giveMana(caster, Color.BLUE, 2)

        driver.castSpell(caster, spell, targets = listOf(drawer)).isSuccess shouldBe true
        driver.bothPass()

        // Resolves fully with no discard decision — nothing connived.
        driver.isPaused shouldBe false
        driver.state.getHand(drawer).contains(opponentDraw) shouldBe true
        // Caster neither drew nor discarded: hand only lost the spell, and the graveyard holds just
        // the resolved instant (no discarded card from a connive that never happened).
        driver.getHandSize(caster) shouldBe (casterHandBefore - 1)
        driver.state.getGraveyard(caster) shouldBe listOf(spell)
        val counters = driver.state.getEntity(creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
