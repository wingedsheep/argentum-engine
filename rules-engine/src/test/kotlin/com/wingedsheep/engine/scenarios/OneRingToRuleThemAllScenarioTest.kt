package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.OneRingToRuleThemAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for One Ring to Rule Them All (LTR #102), a three-chapter Saga.
 *
 * I — The Ring tempts you, then each player mills cards equal to your Ring-bearer's power.
 * II — Destroy all nonlegendary creatures.
 * III — Each opponent loses 1 life for each creature card in that player's graveyard.
 *
 * Chapters are driven the realistic way: cast the Saga (chapter I fires on entry as lore 1 is
 * added) and advance turns so the upkeep lore counter triggers chapters II and III.
 */
class OneRingToRuleThemAllScenarioTest : FunSpec({

    // Power-3 vanilla creature — designated as the Ring-bearer so chapter I mills 3.
    val powerThree = card("Test Power Three") {
        manaCost = "{3}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }
    val vanilla = card("Test Vanilla") {
        manaCost = "{2}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
    }
    val legend = card("Test Legend") {
        manaCost = "{2}"
        typeLine = "Legendary Creature — Elf Noble"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OneRingToRuleThemAll, powerThree, vanilla, legend))
        return driver
    }

    fun GameTestDriver.resolveStack(designateBearer: EntityId? = null) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 80) {
            val decision = state.pendingDecision
            when {
                decision is SelectCardsDecision && designateBearer != null &&
                    designateBearer in decision.options ->
                    submitDecision(decision.playerId, CardsSelectedResponse(decision.id, listOf(designateBearer)))
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    fun GameTestDriver.advanceToMain(targetRound: Int) {
        var guard = 0
        while (!(state.turnNumber == targetRound && state.step == Step.PRECOMBAT_MAIN) && guard < 600) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to round $targetRound")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    /** Cast the Saga as the active player; resolves chapter I, designating [bearer] if prompted. */
    fun GameTestDriver.castSaga(controller: EntityId, bearer: EntityId?): EntityId {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.BLACK, 4)
        val saga = putCardInHand(controller, "One Ring to Rule Them All")
        castSpell(controller, saga)
        resolveStack(designateBearer = bearer)
        return saga
    }

    fun GameTestDriver.graveyardSize(player: EntityId): Int =
        state.getZone(ZoneKey(player, Zone.GRAVEYARD)).size

    test("Chapter I: the Ring tempts you, then each player mills cards equal to your Ring-bearer's power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // A power-3 creature to designate as the Ring-bearer.
        val bearer = driver.putCreatureOnBattlefield(controller, "Test Power Three")

        val gyControllerBefore = driver.graveyardSize(controller)
        val gyOpponentBefore = driver.graveyardSize(opponent)

        driver.castSaga(controller, bearer = bearer)

        // The chosen creature is now the Ring-bearer.
        driver.state.getEntity(bearer)
            ?.get<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()
            ?.ownerId shouldBe controller

        // Each player milled exactly 3 (the Ring-bearer's power) into their graveyard.
        (driver.graveyardSize(controller) - gyControllerBefore) shouldBe 3
        (driver.graveyardSize(opponent) - gyOpponentBefore) shouldBe 3
    }

    test("Chapter II: destroys nonlegendary creatures but not legendary ones") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        // Designate a bearer so chapter I's mill resolves cleanly.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearer = driver.putCreatureOnBattlefield(controller, "Test Power Three")
        driver.castSaga(controller, bearer = bearer)

        // Board to wipe on chapter II.
        val nonlegendary = driver.putCreatureOnBattlefield(opponent, "Test Vanilla")
        val legendary = driver.putCreatureOnBattlefield(opponent, "Test Legend")

        driver.advanceToMain(2) // upkeep adds lore 2 → chapter II triggers
        driver.resolveStack()

        val battlefield = driver.state.getBattlefield()
        (nonlegendary in battlefield) shouldBe false
        (legendary in battlefield) shouldBe true
    }

    test("Chapter III: each opponent loses life equal to creature cards in that opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bearer = driver.putCreatureOnBattlefield(controller, "Test Power Three")
        driver.castSaga(controller, bearer = bearer)

        // Put exactly two creature cards (plus a noncreature) in the opponent's graveyard.
        driver.putCardInGraveyard(opponent, "Test Vanilla")
        driver.putCardInGraveyard(opponent, "Test Legend")
        driver.putCardInGraveyard(opponent, "Mountain") // noncreature — must NOT count

        val opponentLifeBefore = driver.getLifeTotal(opponent)
        val controllerLifeBefore = driver.getLifeTotal(controller)

        driver.advanceToMain(2) // chapter II (board wipe — no creatures, no-op for life)
        driver.resolveStack()
        driver.advanceToMain(3) // chapter III
        driver.resolveStack()

        // Opponent had 2 creature cards in their graveyard → loses 2 life.
        (opponentLifeBefore - driver.getLifeTotal(opponent)) shouldBe 2
        // Controller is not an opponent of themselves — unaffected by chapter III.
        driver.getLifeTotal(controller) shouldBe controllerLifeBefore
    }
})
