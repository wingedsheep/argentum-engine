package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.DiluvianPrimordial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Diluvian Primordial — "When this creature enters, for each opponent, you may cast up to one target
 * instant or sorcery card from that player's graveyard without paying its mana cost. If a spell cast
 * this way would be put into a graveyard, exile it instead."
 *
 * Exercises casting a card the opponent owns out of the opponent's graveyard, the "may" decline,
 * and the exile-instead rider landing the card in its owner's exile.
 */
class DiluvianPrimordialScenarioTest : FunSpec({

    val instant = card("Test Quickening") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "You gain 2 life."
        spell { effect = Effects.GainLife(2) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DiluvianPrimordial, instant))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castPrimordial(driver: GameTestDriver, me: EntityId) {
        val primordial = driver.putCardInHand(me, "Diluvian Primordial")
        driver.giveMana(me, Color.BLUE, 7)
        driver.castSpell(me, primordial).error.shouldBeNull()
        driver.bothPass()
    }

    fun GameTestDriver.resolveStack() {
        repeat(20) {
            if (state.pendingDecision != null || stackSize == 0) return
            bothPass()
        }
    }

    test("casts the opponent's instant for free under your control, then exiles it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val theirs = driver.putCardInGraveyard(opponent, "Test Quickening")
        val mine = driver.putCardInGraveyard(me, "Test Quickening")

        castPrimordial(driver, me)

        val targets = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("only opponents' graveyards are legal") {
            targets.legalTargets.values.flatten() shouldNotContain mine
        }
        driver.submitTargetSelection(me, listOf(theirs)).error.shouldBeNull()
        driver.bothPass()

        val cast = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, listOf(theirs)).error.shouldBeNull()
        driver.getStackSpellNames() shouldBe listOf("Test Quickening")
        driver.resolveStack()

        driver.getLifeTotal(me) shouldBe 22
        driver.getLifeTotal(opponent) shouldBe 20
        withClue("the spell cast this way goes to its owner's exile, not their graveyard") {
            driver.getExileCardNames(opponent) shouldBe listOf("Test Quickening")
            driver.getGraveyard(opponent) shouldNotContain theirs
        }
        driver.getGraveyard(me) shouldContain mine
        cast.options shouldContain theirs
    }

    test("you may decline at resolution, leaving the card in its owner's graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val theirs = driver.putCardInGraveyard(opponent, "Test Quickening")

        castPrimordial(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(theirs)).error.shouldBeNull()
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, emptyList()).error.shouldBeNull()

        driver.getGraveyard(opponent) shouldContain theirs
        driver.getLifeTotal(me) shouldBe 20
    }
})
