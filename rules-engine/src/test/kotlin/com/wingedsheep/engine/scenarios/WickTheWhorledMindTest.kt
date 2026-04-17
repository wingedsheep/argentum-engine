package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.WickTheWhorledMind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test for the bug where Wick, the Whorled Mind's sacrifice ability read the
 * Snail's base power instead of its projected power — a 1/1 Snail buffed to 3/1 by
 * Scales of Shale only dealt 1 damage instead of 3.
 *
 * Per Wick's ruling: "Use the power of the sacrificed Snail as it last existed on the
 * battlefield to determine how much damage to deal and how many cards to draw."
 * (Rule 112.7a / 608.2h — last-known-information for entities that left the battlefield.)
 */
class WickTheWhorledMindTest : FunSpec({

    val sacrificeAbilityId = WickTheWhorledMind.activatedAbilities[0].id

    // Plain 1/1 Snail to sacrifice.
    val TestSnail = CardDefinition.creature(
        name = "Test Snail",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Snail")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestSnail))
        return driver
    }

    test("sacrificed Snail deals damage and draws cards equal to its buffed power") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wick = driver.putPermanentOnBattlefield(activePlayer, "Wick, the Whorled Mind")
        val snail = driver.putCreatureOnBattlefield(activePlayer, "Test Snail") // 1/1

        // Cast Scales of Shale on the Snail: +2/+0, buffs it to 3/1 until end of turn.
        val scales = driver.putCardInHand(activePlayer, "Scales of Shale")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveColorlessMana(activePlayer, 2)
        driver.castSpell(activePlayer, scales, targets = listOf(snail)).isSuccess shouldBe true
        driver.bothPass()

        val handBefore = driver.getHandSize(activePlayer)
        val opponentLifeBefore = driver.getLifeTotal(opponent)

        // Pay {U}{B}{R} and sacrifice the (now 3/1) Snail.
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.RED, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wick,
                abilityId = sacrificeAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(snail))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Snail is sacrificed
        driver.findPermanent(activePlayer, "Test Snail") shouldBe null

        // Opponent takes 3 damage (buffed power), not 1 (base power).
        driver.getLifeTotal(opponent) shouldBe opponentLifeBefore - 3

        // Active player draws 3 cards.
        driver.getHandSize(activePlayer) shouldBe handBefore + 3
    }

    test("sacrificed Snail at base power deals damage equal to base power") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wick = driver.putPermanentOnBattlefield(activePlayer, "Wick, the Whorled Mind")
        val snail = driver.putCreatureOnBattlefield(activePlayer, "Test Snail") // 1/1

        val handBefore = driver.getHandSize(activePlayer)
        val opponentLifeBefore = driver.getLifeTotal(opponent)

        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.RED, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wick,
                abilityId = sacrificeAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(snail))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe opponentLifeBefore - 1
        driver.getHandSize(activePlayer) shouldBe handBefore + 1
    }
})
