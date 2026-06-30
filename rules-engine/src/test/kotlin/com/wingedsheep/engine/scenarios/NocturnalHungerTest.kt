package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.NocturnalHunger
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for Nocturnal Hunger — a Gift a Food instant from Bloomburrow.
 *
 * Mode 0: No gift — destroy target creature, you lose 2 life.
 * Mode 1: Gift — the opponent (not you) creates a Food token, then destroy target creature.
 *
 * Regression: the gift Food used `Player.EachOpponent`, a list-only reference that has no single
 * resolution as a token controller. The token executor fell back to the spell's controller, so the
 * caster received the Food instead of the promised opponent. The fix is `Player.AnOpponent`.
 */
class NocturnalHungerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(NocturnalHunger)
        return driver
    }

    test("mode 1 (gift): the OPPONENT creates the Food token, not the caster") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.giveMana(activePlayer, Color.BLACK, 3)
        val spell = driver.putCardInHand(activePlayer, "Nocturnal Hunger")

        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(theirs)),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(theirs)))
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        // The Food belongs to the opponent — never the caster.
        driver.findPermanent(opponent, "Food").shouldNotBeNull()
        driver.findPermanent(activePlayer, "Food").shouldBeNull()

        // Target creature destroyed; gift was promised so the caster does NOT lose life.
        (theirs in driver.state.getZone(opponent, Zone.GRAVEYARD)) shouldBe true
        driver.getLifeTotal(activePlayer) shouldBe 20
    }

    test("mode 0 (no gift): no Food is created and the caster loses 2 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.giveMana(activePlayer, Color.BLACK, 3)
        val spell = driver.putCardInHand(activePlayer, "Nocturnal Hunger")

        val result = driver.submit(CastSpell(
            playerId = activePlayer,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(theirs)),
            chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(theirs)))
        ))
        result.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Food").shouldBeNull()
        driver.findPermanent(activePlayer, "Food").shouldBeNull()

        (theirs in driver.state.getZone(opponent, Zone.GRAVEYARD)) shouldBe true
        driver.getLifeTotal(activePlayer) shouldBe 18
    }
})
