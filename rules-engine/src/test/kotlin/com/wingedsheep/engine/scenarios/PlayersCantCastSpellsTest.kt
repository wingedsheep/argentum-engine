package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.VoiceOfVictory
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for [PlayersCantCastSpells] — a continuous cast prohibition parameterized along three
 * independent axes (who / which / when), and the single `reasonCannotCast` cast-legality
 * chokepoint that enforces it across every casting zone.
 *
 * Probes are instants and flash creatures so that *baseline* castability isn't masked by the
 * sorcery-speed timing rule (a plain creature/sorcery wouldn't enumerate on an opponent's turn
 * anyway, which would make a restriction look effective when it isn't). `enumerate` returns
 * actions regardless of priority, so presence/absence of a CastSpell action isolates the rule.
 */
class PlayersCantCastSpellsTest : FunSpec({

    // --- Lock permanents (creatures so putCreatureOnBattlefield works; the static is what matters) ---

    // "Your opponents can't cast green spells with mana value 4 or greater." (always)
    val greenBigLock = card("Test Green Warden") {
        manaCost = "{2}{G}"; typeLine = "Creature — Wall"; power = 0; toughness = 4
        staticAbility {
            ability = PlayersCantCastSpells(
                affected = Player.EachOpponent,
                spellFilter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.HasColor(Color.GREEN), CardPredicate.ManaValueAtLeast(4))
                )
            )
        }
    }

    // "You can't cast spells during an opponent's turn."
    val selfOppTurnLock = card("Test Patient Warden") {
        manaCost = "{1}{U}"; typeLine = "Creature — Wall"; power = 0; toughness = 4
        staticAbility { ability = PlayersCantCastSpells(affected = Player.You, condition = IsNotYourTurn) }
    }

    // "Your opponents can't cast creature spells during your turn."
    val oppCreaturesYourTurnLock = card("Test Vigilant Warden") {
        manaCost = "{1}{W}"; typeLine = "Creature — Wall"; power = 0; toughness = 4
        staticAbility {
            ability = PlayersCantCastSpells(
                affected = Player.EachOpponent,
                spellFilter = GameObjectFilter.Creature,
                condition = IsYourTurn
            )
        }
    }

    // "Your opponents can't cast red spells." (always) — used for the zone-coverage check.
    val redLock = card("Test Red Warden") {
        manaCost = "{1}{R}"; typeLine = "Creature — Wall"; power = 0; toughness = 4
        staticAbility {
            ability = PlayersCantCastSpells(
                affected = Player.EachOpponent,
                spellFilter = GameObjectFilter(cardPredicates = listOf(CardPredicate.HasColor(Color.RED)))
            )
        }
    }

    // --- Probe spells ---
    val bigGreen = card("Test Big Bear") { manaCost = "{3}{G}"; typeLine = "Creature — Bear"; power = 4; toughness = 4 }
    val smallGreen = card("Test Cub") { manaCost = "{G}"; typeLine = "Creature — Bear"; power = 1; toughness = 1 }
    val flashBeast = card("Test Flash Beast") {
        manaCost = "{1}{G}"; typeLine = "Creature — Beast"; power = 2; toughness = 2
        keywords(Keyword.FLASH)
    }

    val extraCards = listOf(
        VoiceOfVictory, greenBigLock, selfOppTurnLock, oppCreaturesYourTurnLock, redLock,
        bigGreen, smallGreen, flashBeast
    )

    fun createDriver(startingPlayer: Int = 0): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = startingPlayer)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    // =========================================================================
    // Voice of Victory: affected = EachOpponent, condition = IsYourTurn, filter = Any
    // =========================================================================

    test("opponents can't cast spells during the controller's turn") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        driver.castActionsFor(opponent, bolt) shouldHaveSize 0
    }

    test("the controller can still cast their own spells during their turn") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(controller, "Lightning Bolt")
        driver.giveMana(controller, Color.RED, 1)

        driver.castActionsFor(controller, bolt).isNotEmpty() shouldBe true
    }

    test("opponents can cast spells on their own turn (the restriction is your-turn-only)") {
        val driver = createDriver(startingPlayer = 1)
        val activeOpponent = driver.activePlayer!!
        val voiceController = driver.getOpponent(activeOpponent)

        driver.putCreatureOnBattlefield(voiceController, "Voice of Victory")
        val bolt = driver.putCardInHand(activeOpponent, "Lightning Bolt")
        driver.giveMana(activeOpponent, Color.RED, 1)

        driver.castActionsFor(activeOpponent, bolt).isNotEmpty() shouldBe true
    }

    test("the handler rejects an opponent's cast attempt during the controller's turn") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.putCreatureOnBattlefield(controller, "Voice of Victory")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(controller)

        driver.submitExpectFailure(
            CastSpell(opponent, bolt, targets = listOf(ChosenTarget.Player(controller)))
        )
    }

    // =========================================================================
    // Axis: spell filter — "opponents can't cast green spells with MV >= 4"
    // =========================================================================

    test("a filtered restriction blocks only matching spells (green, MV >= 4)") {
        // The restricted opponent is the active player so the creature probes meet sorcery timing.
        val driver = createDriver(startingPlayer = 1)
        val caster = driver.activePlayer!!
        val lockController = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(lockController, "Test Green Warden")
        val big = driver.putCardInHand(caster, "Test Big Bear")     // green, MV 4 -> blocked
        val small = driver.putCardInHand(caster, "Test Cub")        // green, MV 1 -> allowed
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")   // red       -> allowed
        driver.giveMana(caster, Color.GREEN, 4)
        driver.giveMana(caster, Color.RED, 1)

        driver.castActionsFor(caster, big) shouldHaveSize 0
        driver.castActionsFor(caster, small).isNotEmpty() shouldBe true
        driver.castActionsFor(caster, bolt).isNotEmpty() shouldBe true
    }

    // =========================================================================
    // Axis: affected = You, condition = IsNotYourTurn — "you can't cast during the opponent's turn"
    // =========================================================================

    test("affected=You + IsNotYourTurn locks the controller during the opponent's turn but not the opponent") {
        // player2 is active (the opponent's turn from the lock controller's perspective).
        val driver = createDriver(startingPlayer = 1)
        val opponentTurnPlayer = driver.activePlayer!!
        val lockController = driver.getOpponent(opponentTurnPlayer)

        driver.putCreatureOnBattlefield(lockController, "Test Patient Warden")
        val controllerBolt = driver.putCardInHand(lockController, "Lightning Bolt")
        val opponentBolt = driver.putCardInHand(opponentTurnPlayer, "Lightning Bolt")
        driver.giveMana(lockController, Color.RED, 1)
        driver.giveMana(opponentTurnPlayer, Color.RED, 1)

        driver.castActionsFor(lockController, controllerBolt) shouldHaveSize 0      // you, during their turn
        driver.castActionsFor(opponentTurnPlayer, opponentBolt).isNotEmpty() shouldBe true  // not affected
    }

    // =========================================================================
    // Axes combined: filter = Creature, condition = IsYourTurn
    // =========================================================================

    test("creature-filtered your-turn lock blocks an opponent's flash creature but not their instant") {
        // Lock controller is active (their turn); the opponent could otherwise flash in a creature.
        val driver = createDriver()
        val lockController = driver.activePlayer!!
        val opponent = driver.getOpponent(lockController)

        driver.putCreatureOnBattlefield(lockController, "Test Vigilant Warden")
        val flashCreature = driver.putCardInHand(opponent, "Test Flash Beast")  // creature -> blocked
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")             // instant  -> allowed
        driver.giveMana(opponent, Color.GREEN, 2)
        driver.giveMana(opponent, Color.RED, 1)

        driver.castActionsFor(opponent, flashCreature) shouldHaveSize 0
        driver.castActionsFor(opponent, bolt).isNotEmpty() shouldBe true
    }

    // =========================================================================
    // Zone coverage: the chokepoint matches the spell in any zone, not just the hand.
    // (This is the bug the old per-spell Mana Maze check had — it only ran on hand casts.)
    // =========================================================================

    test("reasonCannotCast blocks a matching card in the graveyard, allowing a non-matching one") {
        val driver = createDriver()
        val lockController = driver.activePlayer!!
        val opponent = driver.getOpponent(lockController)

        driver.putCreatureOnBattlefield(lockController, "Test Red Warden")       // opponents can't cast red
        val redInGraveyard = driver.putCardInGraveyard(opponent, "Lightning Bolt")  // red
        val blueInGraveyard = driver.putCardInGraveyard(opponent, "Counterspell")   // blue

        val utils = CastPermissionUtils(driver.cardRegistry, PredicateEvaluator(), ConditionEvaluator())
        utils.reasonCannotCast(driver.state, opponent, redInGraveyard) shouldNotBe null
        utils.reasonCannotCast(driver.state, opponent, blueInGraveyard) shouldBe null
    }
})
