package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlayer
import com.wingedsheep.engine.state.components.identity.CardComponent
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

    val WordsOfWar = card("Words of War") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"

        activatedAbility {
            cost = Costs.Mana("{1}")
            target = TargetCreatureOrPlayer()
            effect = Effects.ReplaceNextDraw(Effects.DealDamage(2, EffectTarget.ContextTarget(0)))
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

    val abilityId = WordsOfWar.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWar, Inspiration, Concentrate))
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

    // --- promptOnDraw tests ---

    test("draw step prompts to activate Words of War, player accepts, targets creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        val bearId = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        // Give an untapped land for mana
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // turn 1, past skipped draw
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN) // still turn 1
        driver.passPriorityUntil(Step.UPKEEP) // turn 2 upkeep (opponent's turn)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN) // turn 2 postcombat
        driver.passPriorityUntil(Step.UPKEEP) // turn 3 upkeep (active player's turn again)

        driver.state.activePlayerId shouldBe activePlayer

        // Pass through upkeep to reach draw step
        driver.bothPass()

        // Should now be at DRAW with a mana source selection decision
        driver.state.step shouldBe Step.DRAW
        val decision = driver.pendingDecision
        decision shouldNotBe null
        (decision is SelectManaSourcesDecision) shouldBe true
        val manaDecision = decision as SelectManaSourcesDecision
        manaDecision.canDecline shouldBe true

        // Accept: auto-pay to activate Words of War
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Now we should get a target selection decision
        val targetDecision = driver.pendingDecision
        targetDecision shouldNotBe null
        (targetDecision is ChooseTargetsDecision) shouldBe true

        // Select the opponent's Grizzly Bears as target
        driver.submitTargetSelection(activePlayer, listOf(bearId))

        // Draw is replaced with 2 damage to Grizzly Bears (2/2 dies)
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("draw step prompts to activate Words of War, player accepts, targets player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.activePlayerId shouldBe activePlayer

        // Pass through upkeep to reach draw step
        driver.bothPass()

        driver.state.step shouldBe Step.DRAW
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true

        // Accept: auto-pay
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Target the opponent player
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // 2 damage to opponent
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("draw step allows declining Words of War activation") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        // Advance past turn 1 to reach active player's draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step (prompt fires)
        driver.bothPass()
        driver.state.step shouldBe Step.DRAW

        // Decline the activation
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Normal draw happens - no damage
        driver.getLifeTotal(opponent) shouldBe 20

        // Active player drew a card normally
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    test("spell draw prompts to activate Words of War for each draw with target selection") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        val bear1 = driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Grizzly Bears")

        // Give untapped lands for mana to pay for activations
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Concentrate ({2}{U}{U})
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Cast Concentrate (draw 3)
        val concentrate = driver.putCardInHand(activePlayer, "Concentrate")
        driver.castSpell(activePlayer, concentrate)
        driver.bothPass()

        // Draw 1: Prompted to activate Words of War
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Target selection for draw 1 - target opponent player
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // Draw 2: Prompted again
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Target selection for draw 2 - target opponent player
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // Draw 3: Prompted again
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Target selection for draw 3 - target opponent player
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // All 3 draws replaced with 2 damage each = 6 total
        driver.getLifeTotal(opponent) shouldBe 14
    }

    test("spell draw prompt can be declined per-draw with target selection") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")

        // Give untapped land for mana
        driver.putPermanentOnBattlefield(activePlayer, "Mountain")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Inspiration ({3}{U})
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Cast Inspiration (draw 2)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Draw 1: Prompted - DECLINE
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Draw 1 happened normally.
        // Draw 2: Prompted again - ACCEPT
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Target selection for draw 2 - target opponent
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // 1 normal draw + 1 replaced with 2 damage
        driver.getLifeTotal(opponent) shouldBe 18
        // initialHandSize + 1 (putCardInHand) - 1 (cast Inspiration) + 1 (normal draw 1) = initialHandSize + 1
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
    }

    // --- Multiple Words cards tests ---

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

    fun GameTestDriver.countBears(playerId: com.wingedsheep.sdk.model.EntityId): Int {
        return getCreatures(playerId).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == "Bear"
        }
    }

    fun createMultiWordsDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WordsOfWar, WordsOfWilding, Inspiration, Concentrate))
        return driver
    }

    test("multiple Words cards: declining one still prompts for the other on draw step") {
        val driver = createMultiWordsDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Advance to draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step
        driver.bothPass()
        driver.state.step shouldBe Step.DRAW

        // First prompt - decline it (could be either Words card)
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        val firstPrompt = driver.pendingDecision as SelectManaSourcesDecision
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Should get prompted for the other Words card
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        val secondPrompt = driver.pendingDecision as SelectManaSourcesDecision
        secondPrompt.prompt shouldNotBe firstPrompt.prompt // Different card

        // Accept the second one
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // The second prompt could be Words of War (needs targets) or Words of Wilding (no targets)
        if (driver.pendingDecision is ChooseTargetsDecision) {
            // It was Words of War - pick opponent as target
            val opponent = driver.getOpponent(activePlayer)
            driver.submitTargetSelection(activePlayer, listOf(opponent))
            // Draw replaced with damage, no card drawn
            driver.getHandSize(activePlayer) shouldBe initialHandSize
            driver.getLifeTotal(opponent) shouldBe 18
        } else {
            // It was Words of Wilding - draw replaced with Bear
            driver.getHandSize(activePlayer) shouldBe initialHandSize
            driver.countBears(activePlayer) shouldBe 1
        }
    }

    test("multiple Words cards: declining all results in normal draw") {
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
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Advance to draw step on turn 3
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Pass through upkeep to reach draw step
        driver.bothPass()
        driver.state.step shouldBe Step.DRAW

        // Decline first prompt
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Decline second prompt
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Both declined - normal draw happens
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 1
        driver.getLifeTotal(opponent) shouldBe 20
        driver.countBears(activePlayer) shouldBe 0
    }

    test("multiple Words cards: spell draw prompts for each card per draw") {
        val driver = createMultiWordsDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putPermanentOnBattlefield(activePlayer, "Words of War")
        driver.putPermanentOnBattlefield(activePlayer, "Words of Wilding")

        // Give untapped lands for mana to pay for activations
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give mana to cast Inspiration ({3}{U})
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val initialHandSize = driver.getHandSize(activePlayer)

        // Cast Inspiration (draw 2)
        val inspiration = driver.putCardInHand(activePlayer, "Inspiration")
        driver.castSpell(activePlayer, inspiration)
        driver.bothPass()

        // Draw 1: First prompt - decline
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Draw 1: Second prompt (other card) - decline too
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Draw 1 happened normally. Draw 2: First prompt - decline
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Draw 2: Second prompt (other card) - decline too
        (driver.pendingDecision is SelectManaSourcesDecision) shouldBe true
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // Both draws happened normally
        // initialHandSize + 1 (putCardInHand) - 1 (cast Inspiration) + 2 (normal draws) = initialHandSize + 2
        driver.getHandSize(activePlayer) shouldBe initialHandSize + 2
        driver.getLifeTotal(opponent) shouldBe 20
        driver.countBears(activePlayer) shouldBe 0
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
