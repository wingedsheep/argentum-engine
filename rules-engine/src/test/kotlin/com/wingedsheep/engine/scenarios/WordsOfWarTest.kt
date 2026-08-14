package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.WordsOfWar
import com.wingedsheep.mtg.sets.definitions.ons.cards.WordsOfWilding
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Words of War.
 *
 * Words of War: {2}{R}
 * Enchantment
 * {1}: The next time you would draw a card this turn, this enchantment deals 2 damage to any target instead.
 */
class WordsOfWarTest : FunSpec({

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

    val abilityId = WordsOfWar.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Inspiration, Concentrate))
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


    test("manual activation shield replaces draw during the draw step") {
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

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate Words of War targeting opponent's creature
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(bearId))
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) — 1st draw replaced with 2 damage, 2nd normal
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Grizzly Bears (2/2) killed by the 2 damage
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
    }

    test("unactivated Words of War does nothing during draw step and disappears if not used") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val wordsId = driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        // Advance to draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step — normal draw
        driver.bothPass()

        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1

        // Activate once
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        driver.currentStep shouldBe Step.DRAW
        driver.state.floatingEffects.size shouldBe 1
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.floatingEffects.size shouldBe 0
    }

    test("shield only replaces one draw per activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate once
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wordsId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) — 1 draw replaced, 1 normal
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        val handSizeBeforeCast = driver.getHandSize(activePlayer)
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getHandSize(activePlayer) shouldBe handSizeBeforeCast
    }

    test("activating for each draw of a multi-draw spell shields each draw") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wordsId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate 3 times to shield 3 draws
        driver.giveMana(activePlayer, Color.RED, 3)
        repeat(3) {
            driver.submitSuccess(
                ActivateAbility(
                    playerId = activePlayer,
                    sourceId = wordsId,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(opponent))
                )
            )
            driver.bothPass()
        }

        // Cast Concentrate (draw 3) — all 3 replaced
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val concentrate = driver.putCardInHand(activePlayer, "Concentrate")
        driver.castSpell(activePlayer, concentrate)
        driver.bothPass()

        // 3 × 2 damage = 6
        driver.getLifeTotal(opponent) shouldBe 14
    }

    // --- Multiple Words cards tests ---

    fun GameTestDriver.countBears(playerId: EntityId): Int {
        return getCreatures(playerId).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == "Bear Token"
        }
    }

    fun createMultiWordsDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Inspiration, Concentrate))
        return driver
    }

    test("multiple Words cards: activating one replaces the draw step draw") {
        val driver = createMultiWordsDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warId = driver.findPermanent(activePlayer, "Words of War")!!

        // Activate only Words of War
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = warId,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) — 1 draw replaced, 1 normal
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Words of War replaced one draw with 2 damage
        driver.getLifeTotal(opponent) shouldBe 18
        // Words of Wilding (unactivated) did nothing
        driver.countBears(activePlayer) shouldBe 0
    }

    test("multiple Words cards: both activated stack shields for spell draws") {
        val driver = createMultiWordsDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")

        // Lands for mana
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warId = driver.findPermanent(activePlayer, "Words of War")!!
        val wildingId = driver.findPermanent(activePlayer, "Words of Wilding")!!
        val warAbility = WordsOfWar.activatedAbilities.first().id
        val wildingAbility = WordsOfWilding.activatedAbilities.first().id

        // Activate both Words cards
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = warId,
                abilityId = warAbility,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        driver.bothPass()
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wildingId,
                abilityId = wildingAbility,
                targets = emptyList()
            )
        )
        driver.bothPass()

        // Cast Inspiration (draw 2) — both draws replaced by one shield each
        driver.giveMana(activePlayer, Color.BLUE, 4)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Two different shields (War + Wilding) — engine pauses for choice (CR 616.1e)
        val warIdx = (driver.pendingDecision as ChooseOptionDecision)
            .options.indexOfFirst { it.contains("War", ignoreCase = true) }
        driver.submitDecision(activePlayer, OptionChosenResponse(driver.pendingDecision!!.id, warIdx))

        // Words of War replaced draw 1 with 2 damage to opponent
        driver.getLifeTotal(opponent) shouldBe 18
        // Words of Wilding replaced draw 2 with a Bear token
        driver.countBears(activePlayer) shouldBe 1
    }

    test("draw step does not prompt when Words of War activation is not affordable") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        // No lands or mana sources - can't afford {1}

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

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
