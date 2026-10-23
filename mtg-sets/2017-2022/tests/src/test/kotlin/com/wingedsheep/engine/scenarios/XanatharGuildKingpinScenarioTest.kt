package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.afr.cards.XanatharGuildKingpin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Xanathar, Guild Kingpin (AFR) — "At the beginning of your upkeep, choose target opponent. Until
 * end of turn, that player can't cast spells, you may look at the top card of their library any
 * time, you may play the top card of their library, and you may spend mana as though it were mana
 * of any color to cast spells this way."
 */
class XanatharGuildKingpinScenarioTest : FunSpec({

    class Game(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val xanathar: EntityId)

    // Both players run only Swamps, so the green and red cards below are castable by `you` only
    // through Xanathar's "as though it were mana of any color".
    fun newGame(): Game {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + XanatharGuildKingpin)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val xanathar = driver.putCreatureOnBattlefield(you, "Xanathar, Guild Kingpin")
        return Game(driver, you, opponent, xanathar)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null) driver.bothPass()
    }

    /** Advance to your next upkeep, aim the trigger at the opponent and resolve it. */
    fun resolveUpkeepTrigger(game: Game) {
        val driver = game.driver
        driver.passPriorityUntil(Step.UPKEEP, game.you)
        if (driver.state.pendingDecision != null) {
            driver.submitTargetSelection(game.you, listOf(game.opponent)).error shouldBe null
        }
        resolveStack(driver)
    }

    fun libraryActionCardIds(driver: GameTestDriver, playerId: EntityId) =
        driver.legalActions(playerId).filter { it.sourceZone == "LIBRARY" }.mapNotNull {
            when (val action = it.action) {
                is CastSpell -> action.cardId
                is PlayLand -> action.cardId
                else -> null
            }
        }

    fun visibleTo(driver: GameTestDriver, viewer: EntityId, cardId: EntityId): Boolean =
        cardId in ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(driver.state, viewingPlayerId = viewer).cards

    test("you see and may cast the opponent's top card, paying with mana of any color; it stays theirs") {
        val game = newGame()
        val driver = game.driver
        resolveUpkeepTrigger(game)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, game.you)
        val courser = driver.putCardOnTopOfLibrary(game.opponent, "Centaur Courser") // {2}{G}
        repeat(3) { driver.putLandOnBattlefield(game.you, "Swamp") }

        visibleTo(driver, game.you, courser) shouldBe true
        visibleTo(driver, game.opponent, courser) shouldBe false
        (courser in libraryActionCardIds(driver, game.you)) shouldBe true

        driver.castSpell(game.you, courser).error shouldBe null
        resolveStack(driver)

        val permanent = driver.findPermanent(game.you, "Centaur Courser")
        (permanent != null) shouldBe true
        driver.getController(permanent!!) shouldBe game.you
        driver.state.getEntity(permanent)?.get<OwnerComponent>()?.playerId shouldBe game.opponent
    }

    test("the permission follows the new top card after one is played") {
        val game = newGame()
        val driver = game.driver
        resolveUpkeepTrigger(game)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, game.you)
        val second = driver.putCardOnTopOfLibrary(game.opponent, "Lightning Bolt")
        val first = driver.putCardOnTopOfLibrary(game.opponent, "Swamp")

        (first in libraryActionCardIds(driver, game.you)) shouldBe true
        (second in libraryActionCardIds(driver, game.you)) shouldBe false
        visibleTo(driver, game.you, second) shouldBe false

        // Playing a land from their library uses your land drop and puts it under your control.
        val played = driver.playLand(game.you, first)
        played.error shouldBe null
        driver.getController(first) shouldBe game.you
        // The zone change reports the card's real owner, so "a land an opponent owns" reads it right.
        played.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == first }.ownerId shouldBe game.opponent
        driver.state.getEntity(first)?.get<OwnerComponent>()?.playerId shouldBe game.opponent

        visibleTo(driver, game.you, second) shouldBe true
        (second in libraryActionCardIds(driver, game.you)) shouldBe true
    }

    test("a land from their library still needs a land play") {
        val game = newGame()
        val driver = game.driver
        resolveUpkeepTrigger(game)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, game.you)
        val ownLand = driver.putCardInHand(game.you, "Swamp")
        driver.playLand(game.you, ownLand).error shouldBe null
        val theirLand = driver.putCardOnTopOfLibrary(game.opponent, "Swamp")

        (theirLand in libraryActionCardIds(driver, game.you)) shouldBe false
        driver.submitExpectFailure(PlayLand(game.you, theirLand))
    }

    test("the targeted opponent can't cast spells this turn") {
        val game = newGame()
        val driver = game.driver
        resolveUpkeepTrigger(game)
        val bolt = driver.putCardInHand(game.opponent, "Lightning Bolt")
        driver.giveMana(game.opponent, Color.RED, 1)
        driver.passPriority(game.you)
        driver.state.priorityPlayerId shouldBe game.opponent

        driver.submitExpectFailure(CastSpell(game.opponent, bolt, targets = listOf(ChosenTarget.Player(game.you))))
        driver.legalActions(game.opponent).any { (it.action as? CastSpell)?.cardId == bolt } shouldBe false
    }

    test("the permission outlasts Xanathar and ends with the turn") {
        val game = newGame()
        val driver = game.driver
        resolveUpkeepTrigger(game)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, game.you)
        driver.moveToGraveyard(game.xanathar)
        val courser = driver.putCardOnTopOfLibrary(game.opponent, "Centaur Courser")
        repeat(3) { driver.putLandOnBattlefield(game.you, "Swamp") }

        // A resolved effect, not Xanathar's static: it survives Xanathar leaving.
        visibleTo(driver, game.you, courser) shouldBe true
        (courser in libraryActionCardIds(driver, game.you)) shouldBe true

        // Into the opponent's turn: the grant expired at cleanup.
        driver.passPriorityUntil(Step.UPKEEP, game.opponent)
        visibleTo(driver, game.you, courser) shouldBe false
        (courser in libraryActionCardIds(driver, game.you)) shouldBe false
    }
})
