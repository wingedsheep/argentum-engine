package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.TheHuntsmansRedemption
import com.wingedsheep.mtg.sets.definitions.woe.cards.TheWitchsVanity
import com.wingedsheep.mtg.sets.definitions.woe.cards.WelcomeToSweettooth
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chapter-by-chapter tests for the three Wilds of Eldraine Sagas in this batch.
 *
 *  - **Welcome to Sweettooth** — I Human token, II Food token, III `X = 1 + Foods you control`
 *    +1/+1 counters. The X calculation is the interesting part: it's a
 *    [com.wingedsheep.sdk.scripting.values.DynamicAmount.Add] over a battlefield Food count, so the
 *    Food minted by chapter II must be visible to chapter III two turns later.
 *  - **The Witch's Vanity** — I destroys a mana-value-2-or-less creature an opponent controls,
 *    II Food token, III Wicked Role attached to your creature.
 *  - **The Huntsman's Redemption** — I 3/3 Beast token, II the optional "sacrifice a creature →
 *    tutor a creature or basic land", III up-to-two creatures get +2/+2 and trample.
 *
 * All three are cast on turn 1, so chapter II resolves on turn 2 and chapter III on turn 3. A
 * chapter's targeting decision surfaces at the start of that turn's precombat main, which is where
 * [advanceToMain] hands off to a drain helper.
 */
class WoeSagasScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + PredefinedTokens.allTokens + listOf(
                WelcomeToSweettooth,
                TheWitchsVanity,
                TheHuntsmansRedemption,
            )
        )
        return driver
    }

    /** Drain the stack, auto-answering anything that pauses. */
    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /**
     * Drain the stack, answering every target request with [targets] — the Saga chapters that target
     * would otherwise get an arbitrary auto-pick.
     */
    fun GameTestDriver.drainTargeting(chooser: EntityId, targets: List<EntityId>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(chooser, targets)
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    /**
     * Advance to the precombat main phase of the starting player's [nth] turn — the clock a Saga's
     * lore counters run on. `GameState.turnNumber` counts player turns, and this is a duel where
     * the two seats alternate, so the starting player's nth turn is turn `2n - 1`.
     */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    fun GameTestDriver.plusOnePlusOne(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.countPermanentsNamed(playerId: EntityId, name: String): Int =
        getPermanents(playerId).count { getCardName(it) == name }

    test("Welcome to Sweettooth: Human, then Food, then X = 1 + Foods you control counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.GREEN, 2)
        val saga = driver.putCardInHand(controller, "Welcome to Sweettooth")
        driver.castSpell(controller, saga)
        driver.drain() // enters with lore 1 → chapter I

        withClue("chapter I creates a 1/1 white Human token") {
            driver.countPermanentsNamed(controller, "Human Token") shouldBe 1
        }

        driver.advanceToMain(2) // lore 2 → chapter II
        driver.drain()

        withClue("chapter II creates a Food token") {
            driver.countPermanentsNamed(controller, "Food") shouldBe 1
        }

        // Chapter III's X counts every Food artifact we control, so add a second one by hand to
        // prove the count is read from the battlefield rather than hard-coded.
        driver.putPermanentOnBattlefield(controller, "Food")

        val human = driver.getPermanents(controller).first { driver.getCardName(it) == "Human Token" }
        driver.advanceToMain(3) // lore 3 → chapter III, which targets
        driver.drainTargeting(controller, listOf(human))

        withClue("X is one plus the two Foods we control, so the Human gets three +1/+1 counters") {
            driver.plusOnePlusOne(human) shouldBe 3
        }
        withClue("a three-chapter Saga is sacrificed after its last chapter resolves") {
            driver.getGraveyardCardNames(controller).contains("Welcome to Sweettooth") shouldBe true
        }
    }

    test("The Witch's Vanity: destroy a small creature, then Food, then a Wicked Role") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.BLACK, 2)
        val saga = driver.putCardInHand(controller, "The Witch's Vanity")
        driver.castSpell(controller, saga)
        driver.drainTargeting(controller, listOf(bear))

        withClue("Grizzly Bears is mana value 2, so chapter I destroys it") {
            driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true
        }

        driver.advanceToMain(2)
        driver.drain()
        withClue("chapter II creates a Food token") {
            driver.countPermanentsNamed(controller, "Food") shouldBe 1
        }

        val ours = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        driver.advanceToMain(3)
        driver.drainTargeting(controller, listOf(ours))

        withClue("chapter III attaches a Wicked Role to our creature, making the 2/2 a 3/3") {
            driver.findPermanent(controller, "Wicked Role") shouldNotBe null
            driver.state.projectedState.getPower(ours) shouldBe 3
            driver.state.projectedState.getToughness(ours) shouldBe 3
        }
    }

    test("The Huntsman's Redemption: Beast token, sacrifice-to-tutor, then +2/+2 and trample") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.GREEN, 3)
        val saga = driver.putCardInHand(controller, "The Huntsman's Redemption")
        driver.castSpell(controller, saga)
        driver.drain() // chapter I

        val beast = driver.findPermanent(controller, "Beast Token")
        withClue("chapter I creates a 3/3 green Beast token") {
            beast shouldNotBe null
            driver.state.projectedState.getPower(beast!!) shouldBe 3
            driver.state.projectedState.getToughness(beast) shouldBe 3
        }

        // Chapter II: accept the optional sacrifice, feed it the Beast, and tutor a basic land.
        val handBefore = driver.getHandSize(controller)
        driver.advanceToMain(2)
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) && guard < 60) {
            val decision = driver.state.pendingDecision
            when {
                decision is YesNoDecision -> driver.submitYesNo(controller, true)
                decision is SelectCardsDecision && decision.options.contains(beast) ->
                    driver.submitCardSelection(controller, listOf(beast!!))
                decision != null -> driver.autoResolveDecision()
                else -> driver.bothPass()
            }
            guard++
        }

        withClue("the Beast was sacrificed to chapter II (a token, so it then ceases to exist)") {
            driver.findPermanent(controller, "Beast Token") shouldBe null
        }
        withClue("sacrificing tutors a creature or basic land into hand (the deck is all Forests)") {
            driver.getHandSize(controller) shouldBe handBefore + 1
        }

        val bear = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        driver.advanceToMain(3) // chapter III
        driver.drainTargeting(controller, listOf(bear))

        withClue("the chosen creature gets +2/+2 and trample until end of turn") {
            driver.state.projectedState.getPower(bear) shouldBe 4
            driver.state.projectedState.getToughness(bear) shouldBe 4
            driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
        }
    }
})
