package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Words of Wilding.
 *
 * Words of Wilding: {2}{G}
 * Enchantment
 * {1}: The next time you would draw a card this turn, create a 2/2 green Bear creature token instead.
 */
class WordsOfWildingTest : FunSpec({

    val WordsOfWilding = card("Words of Wilding") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.ReplaceNextDraw(
                Effects.CreateToken(power = 2, toughness = 2, colors = setOf(Color.GREEN), creatureTypes = setOf("Bear"))
            )
            promptOnDraw = true
        }
    }

    // A simple draw spell for testing
    val Inspiration = CardDefinition.instant(
        name = "Inspiration",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Draw two cards.",
        script = CardScript.spell(effect = DrawCardsEffect(2))
    )

    // A draw-3 spell for testing multi-draw prompt
    val Concentrate = CardDefinition.sorcery(
        name = "Concentrate",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        oracleText = "Draw three cards.",
        script = CardScript.spell(effect = DrawCardsEffect(3))
    )

    val abilityId = WordsOfWilding.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWilding, Inspiration, Concentrate))
        return driver
    }

    fun GameTestDriver.countBears(playerId: com.wingedsheep.sdk.model.EntityId): Int {
        return getCreatures(playerId).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == "Bear"
        }
    }

    test("activating Words of Wilding replaces next draw with a 2/2 Bear token") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana for activation ({1}) and for Inspiration ({3}{U})
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wilding")!!
        val initialHandSize = driver.getHandSize(activePlayer)

        // Activate Words of Wilding
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

        // First draw was replaced with Bear token, second draw proceeded normally
        // Hand: initialHandSize + 1 (Inspiration put in hand) - 1 (cast Inspiration) + 1 (normal 2nd draw) = initialHandSize + 1
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1

        // Should have a Bear token on the battlefield
        val bear = driver.findPermanent(activePlayer, "Bear")
        bear shouldNotBe null
        val bearCard = driver.state.getEntity(bear!!)!!.get<CardComponent>()!!
        bearCard.baseStats shouldBe com.wingedsheep.sdk.model.CreatureStats(2, 2)
        bearCard.colors shouldBe setOf(Color.GREEN)
    }

    test("Words of Wilding shield only replaces one draw from a multi-draw spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wilding")!!

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

        // 1st draw replaced with Bear, 2nd draw normal
        // handSizeBeforeCast - 1 (cast Inspiration) + 1 (normal 2nd draw) = handSizeBeforeCast
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast

        // Should have exactly one Bear token
        driver.countBears(activePlayer) shouldBe 1
    }

    test("activating multiple times stacks shields for multiple draws") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val wordsId = driver.findPermanent(activePlayer, "Words of Wilding")!!

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

        // Both draws replaced with Bear tokens - no cards drawn
        // handSizeBeforeCast - 1 (cast Inspiration) + 0 (both draws replaced) = handSizeBeforeCast - 1
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast - 1

        // Should have two Bear tokens on the battlefield
        driver.countBears(activePlayer) shouldBe 2
    }

    test("Words of Wilding shield expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Activate during main phase
        driver.giveMana(activePlayer, Color.GREEN, 1)
        val wordsId = driver.findPermanent(activePlayer, "Words of Wilding")!!
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

        // Advance past current main phase first
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // Now advance to the next precombat main (goes through end step, cleanup, opponent's turn)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // That was opponent's main. Now advance to our next main.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Shield should have expired at the end of our first turn
        driver.state.floatingEffects.size shouldBe 0

        // No Bear tokens should have been created (shield expired unused)
        driver.countBears(activePlayer) shouldBe 0
    }

    // --- promptOnDraw tests ---

    test("draw step prompts to activate Words of Wilding, player accepts") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.activePlayerId shouldBe activePlayer
        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step
        driver.bothPass()

        // Should now be at DRAW with a mana source selection decision
        driver.state.step shouldBe Step.DRAW
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true

        // Accept: auto-pay to activate Words of Wilding
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Draw replaced with Bear token, no card drawn
        driver.getHandSize(activePlayer) shouldBe initialHandSize
        driver.countBears(activePlayer) shouldBe 1
    }

    test("draw step allows declining Words of Wilding activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step
        driver.bothPass()
        driver.state.step shouldBe Step.DRAW

        // Decline the activation
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Normal draw happens - no Bear token
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
        driver.countBears(activePlayer) shouldBe 0
    }

    test("spell draw prompts to activate Words of Wilding for each draw") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")

        // Give untapped lands for mana to pay for activations
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Concentrate ({2}{U}{U})
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Cast Concentrate (draw 3)
        val concentrate = driver.putCardInHand(activePlayer, "Concentrate")
        driver.castSpell(activePlayer, concentrate)
        driver.bothPass()

        // Draw 1: Prompted - ACCEPT
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Draw 2: Prompted - ACCEPT
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Draw 3: Prompted - ACCEPT
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // All 3 draws replaced with Bear tokens
        driver.countBears(activePlayer) shouldBe 3
    }
})
