package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithLifeGainEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Words of Worship.
 *
 * Words of Worship: {2}{W}
 * Enchantment
 * {1}: The next time you would draw a card this turn, you gain 5 life instead.
 */
class WordsOfWorshipTest : FunSpec({

    val WordsOfWorship = card("Words of Worship") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = ReplaceNextDrawWithLifeGainEffect(lifeAmount = 5)
        }
    }

    // A simple draw spell for testing
    val Inspiration = CardDefinition.instant(
        name = "Inspiration",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(effect = DrawCardsEffect(2))
    )

    val abilityId = WordsOfWorship.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWorship, Inspiration))
        return driver
    }

    test("activating Words of Worship replaces next draw with 5 life gain") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Worship")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana for activation ({1}) and for Inspiration ({3}{U})
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Worship")!!
        val initialLife = driver.getLifeTotal(activePlayer)
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of Worship
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // First draw was replaced with 5 life, second draw proceeded normally
        driver.getLifeTotal(activePlayer) shouldBe initialLife + 5
        // initialHandSize + 1 card drawn normally - 1 Inspiration cast + 1 Inspiration put in hand
        // Wait: putCardInHand adds 1 to hand, castSpell removes 1 from hand, then draw effect:
        // 1st draw replaced (no card), 2nd draw adds 1 card = net +0 from cast
        // So hand size should be initialHandSize + 1 (drew 1 card from the 2nd draw)
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    test("Words of Worship shield only replaces one draw from a multi-draw spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Worship")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Worship")!!
        val initialLife = driver.getLifeTotal(activePlayer)

        // Activate once
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - only 1st draw is replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // 1st draw replaced with +5 life, 2nd draw normal
        driver.getLifeTotal(activePlayer) shouldBe initialLife + 5
        // handSizeBeforeCast - 1 (cast Inspiration) + 1 (normal 2nd draw) = handSizeBeforeCast
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast
    }

    test("activating multiple times stacks shields for multiple draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Worship")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.WHITE, 2)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Worship")!!
        val initialLife = driver.getLifeTotal(activePlayer)

        // Activate twice to create two shields
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - both draws are replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Both draws replaced with +5 life each = +10 total
        driver.getLifeTotal(activePlayer) shouldBe initialLife + 10
        // handSizeBeforeCast - 1 (cast Inspiration) + 0 (both draws replaced) = handSizeBeforeCast - 1
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast - 1
    }

    test("Words of Worship shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Worship")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val initialLife = driver.getLifeTotal(activePlayer)

        // Activate during main phase
        driver.giveMana(activePlayer, Color.WHITE, 1)
        val wordsId = driver.findPermanent(activePlayer, "Words of Worship")!!
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Verify shield exists
        driver.state.floatingEffects.size shouldBe 1

        // Advance past current main phase first (we're AT precombat main)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // Now advance to the next precombat main (goes through end step, cleanup, opponent's turn)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // That was opponent's main. Now advance to our next main.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Shield should have expired at the end of our first turn
        driver.state.floatingEffects.size shouldBe 0

        // Life should not have changed (shield expired without being used)
        driver.getLifeTotal(activePlayer) shouldBe initialLife
    }
})
