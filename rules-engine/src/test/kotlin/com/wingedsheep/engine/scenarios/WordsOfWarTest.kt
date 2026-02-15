package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithDamageEffect
import com.wingedsheep.sdk.targeting.TargetCreatureOrPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Words of War.
 *
 * Words of War: {2}{R}
 * Enchantment
 * {1}: The next time you would draw a card this turn, this enchantment deals 2 damage to any target instead.
 */
class WordsOfWarTest : FunSpec({

    val WordsOfWar = card("Words of War") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            target = TargetCreatureOrPlayer()
            effect = ReplaceNextDrawWithDamageEffect(damageAmount = 2)
        }
    }

    // A simple draw spell for testing
    val Inspiration = CardDefinition.instant(
        name = "Inspiration",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(effect = DrawCardsEffect(2))
    )

    val abilityId = WordsOfWar.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWar, Inspiration))
        return driver
    }

    test("activating Words of War replaces next draw with 2 damage to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        val bearId = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of War targeting the opponent's creature
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bearId))
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // First draw was replaced with 2 damage to Grizzly Bears, second draw proceeded normally
        // Grizzly Bears is 2/2, so 2 damage should kill it
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null

        // Hand size: initial + 1 (putCardInHand) - 1 (cast Inspiration) + 1 (second draw) = initial + 1
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    test("activating Words of War replaces next draw with 2 damage to target player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate Words of War targeting the opponent
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Cast Inspiration to draw 2 cards
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // 2 damage to opponent
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("Words of War shield only replaces one draw from a multi-draw spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate once
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - only 1st draw is replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // 1st draw replaced with 2 damage, 2nd draw normal
        driver.getLifeTotal(opponent) shouldBe 18
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
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.RED, 2)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate twice targeting opponent
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) - both draws are replaced
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Both draws replaced with 2 damage each = 4 total
        driver.getLifeTotal(opponent) shouldBe 16
        // handSizeBeforeCast - 1 (cast Inspiration) + 0 (both draws replaced) = handSizeBeforeCast - 1
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast - 1
    }

    test("Words of War shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Activate during main phase
        driver.giveMana(activePlayer, Color.RED, 1)
        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Verify shield exists
        driver.state.floatingEffects.size shouldBe 1

        // Advance past current turn
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Shield should have expired at the end of our first turn
        driver.state.floatingEffects.size shouldBe 0

        // Life should not have changed (shield expired without being used)
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
