package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.WordsOfWind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
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

    // A creature with a tap-to-draw-3 ability (like Arcanis the Omnipotent)
    val DrawThreeCreature = card("Draw Three Creature") {
        manaCost = "{3}{U}{U}{U}"
        typeLine = "Creature — Wizard"
        power = 3
        toughness = 4

        activatedAbility {
            cost = AbilityCost.Tap
            effect = DrawCardsEffect(3)
        }
    }

    val abilityId = WordsOfWind.activatedAbilities.first().id

    val drawThreeAbilityId = DrawThreeCreature.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Inspiration, Concentrate, DrawThreeCreature))
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

    /**
     * Resolve all bounce decisions, preferring to bounce permanents with a given name.
     * This avoids accidentally bouncing Words of Wind itself.
     */
    fun GameTestDriver.resolveAllBounceDecisionsPreferring(preferredName: String) {
        while (pendingDecision is SelectCardsDecision) {
            val decision = pendingDecision as SelectCardsDecision
            val preferred = decision.options.firstOrNull { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.name == preferredName
            }
            submitCardSelection(decision.playerId, listOf(preferred ?: decision.options.first()))
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

    test("manual activation — shield replaces draw during the draw step") {
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

        val wordsId = driver.findPermanent(activePlayer, "Words of Wind")!!

        // Activate Words of Wind during the main phase
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) — 1st draw replaced with bounce, 2nd normal
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Active player chooses a permanent to bounce
        driver.submitCardSelection(activePlayer, listOf(activeBear))

        // Opponent's Grizzly Bears auto-bounced (only 1 permanent)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(activePlayer, "Words of Wind") shouldNotBe null
    }

    test("unactivated Words of Wind does nothing during draw step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step — normal draw, no prompt
        driver.bothPass()

        // Normal draw happened
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    test("draw step does not prompt when Words of Wind activation is not affordable") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of Wind")
        // No lands or mana sources - can't afford {1}

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // turn 1, past skipped draw
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN) // still turn 1
        driver.passPriorityUntil(Step.UPKEEP) // turn 2 upkeep (opponent's turn)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN) // turn 2 postcombat
        driver.passPriorityUntil(Step.UPKEEP) // turn 3 upkeep (active player's turn)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step - should NOT prompt (can't afford)
        driver.bothPass()

        // No mana selection decision - draw happens normally
        val decision = driver.pendingDecision
        (decision is SelectManaSourcesDecision) shouldBe false

        // Normal draw happened
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }
})
