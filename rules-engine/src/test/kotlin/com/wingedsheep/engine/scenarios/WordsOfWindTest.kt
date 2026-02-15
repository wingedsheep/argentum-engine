package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
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
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithBounceEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Words of Wind.
 *
 * Words of Wind: {2}{U}
 * Enchantment
 * {1}: The next time you would draw a card this turn, each player returns a
 * permanent they control to its owner's hand instead.
 */
class WordsOfWindTest : FunSpec({

    val WordsOfWind = card("Words of Wind") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = ReplaceNextDrawWithBounceEffect
        }
    }

    // A simple draw spell for testing
    val Inspiration = CardDefinition.instant(
        name = "Inspiration",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(effect = DrawCardsEffect(2))
    )

    val abilityId = WordsOfWind.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWind, Inspiration))
        return driver
    }

    /**
     * Helper to resolve all pending bounce decisions by always selecting the first option.
     */
    fun GameTestDriver.resolveAllBounceDecisions() {
        while (pendingDecision is SelectCardsDecision) {
            val decision = pendingDecision as SelectCardsDecision
            submitCardSelection(decision.playerId, listOf(decision.options.first()))
        }
    }

    test("activating Words of Wind replaces next draw with each-player bounce") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        val activeBear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLUE, 5)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of Wind
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

        // First draw is replaced with bounces.
        // Active player has 2 permanents (Words of Wind + Grizzly Bears) - needs to choose.
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe activePlayer
        driver.submitCardSelection(activePlayer, listOf(activeBear))

        // Opponent has 1 permanent (Grizzly Bears) - auto-selected and bounced.
        // Second draw proceeds normally.

        // Active player: Words of Wind stays, Grizzly Bears bounced
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.findPermanent(activePlayer, "Words of Wind") shouldNotBe null

        // Opponent: Grizzly Bears bounced
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null

        // Active player hand: initialHandSize + 1 (Grizzly Bears bounced) + 1 (2nd draw normal)
        // (Inspiration was added by putCardInHand then removed by castSpell, net 0)
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 2
    }

    test("Words of Wind shield only replaces one draw from a multi-draw spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        val activeBear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLUE, 5)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!

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
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Active player needs to choose a permanent to bounce (2 permanents)
        driver.submitCardSelection(activePlayer, listOf(activeBear))

        // 1st draw replaced with bounces, 2nd draw normal
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
    }

    test("activating multiple times stacks bounce shields for multiple draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        val activeBear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLUE, 6)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!

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

        // Cast Inspiration (draw 2) - both draws are replaced with bounces
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Resolve all bounce decisions (selecting first available permanent each time)
        driver.resolveAllBounceDecisions()

        // After both bounces:
        // Words of Wind should be bounced (during second bounce it's the only active player permanent)
        driver.findPermanent(activePlayer, "Words of Wind") shouldBe null
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null

        // Opponent: both Grizzly Bears bounced
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
    }

    test("Words of Wind shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Activate during main phase
        driver.giveMana(activePlayer, Color.BLUE, 1)
        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!
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

        // Advance through turns
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Shield should have expired
        driver.state.floatingEffects.size shouldBe 0
    }

    test("Words of Wind bounces Words itself when it is the only permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        // No other permanents - opponent has none, active player only has Words

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.BLUE, 5)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // 1st draw replaced: Words of Wind is active player's only permanent, auto-bounced
        // Opponent has no permanents, skipped
        // 2nd draw: normal
        driver.findPermanent(activePlayer, "Words of Wind") shouldBe null

        // Active player: initialHandSize + 1 (Words bounced) + 1 (normal 2nd draw)
        // (Inspiration was added by putCardInHand then removed by castSpell, net 0)
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 2
    }
})
