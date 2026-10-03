package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.ShadowsOverInnistradSet
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Erdwal Illuminator (SOI) — {1}{U} Creature — Spirit 1/3
 *
 * "Flying
 *  Whenever you investigate for the first time each turn, investigate an additional time."
 *
 * Pins the rules of the trigger: it watches the investigate action (CR 701.16a), not Clue creation;
 * only the turn's first investigate fires it, so its own extra investigate and a second investigate
 * later the same turn don't; "investigate twice" is two investigates but still one trigger; and the
 * per-turn marker resets, so the next turn's first investigate fires it again.
 */
class ErdwalIlluminatorScenarioTest : FunSpec({

    val investigateSpell = card("Test Investigate") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.Investigate() }
    }
    val investigateTwiceSpell = card("Test Investigate Twice") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.Investigate(2) }
    }
    val clueSpell = card("Test Clue Maker") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.CreateClue() }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShadowsOverInnistradSet.cards)
        driver.registerCard(PredefinedTokens.Clue)
        driver.registerCards(listOf(investigateSpell, investigateTwiceSpell, clueSpell))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.clues(playerId: EntityId): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }

    /** Cast [name] and resolve it plus any trigger it causes. */
    fun GameTestDriver.castAndResolve(playerId: EntityId, name: String) {
        val spell = putCardInHand(playerId, name)
        giveMana(playerId, Color.BLUE, 1)
        castSpell(playerId, spell).outcome shouldBe Outcome.Done
        bothPass() // the spell resolves
        while (state.stack.isNotEmpty()) bothPass() // the Illuminator's trigger, if any
    }

    test("the first investigate of the turn investigates an additional time") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        driver.castAndResolve(me, "Test Investigate")

        driver.clues(me) shouldBe 2
    }

    test("an investigating enters trigger (Thraben Inspector) fires it too") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        val inspector = driver.putCardInHand(me, "Thraben Inspector")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, inspector).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.clues(me) shouldBe 2
    }

    test("a second investigate the same turn does not trigger it again") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        driver.castAndResolve(me, "Test Investigate")
        driver.clues(me) shouldBe 2

        driver.castAndResolve(me, "Test Investigate")
        driver.clues(me) shouldBe 3
    }

    test("investigate twice is two investigates but only the first one triggers it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        driver.castAndResolve(me, "Test Investigate Twice")

        driver.clues(me) shouldBe 3
    }

    test("creating a Clue token is not investigating") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        driver.castAndResolve(me, "Test Clue Maker")
        driver.clues(me) shouldBe 1

        // …and it didn't use up the turn's first investigate either.
        driver.castAndResolve(me, "Test Investigate")
        driver.clues(me) shouldBe 3
    }

    test("an opponent investigating does not trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        val spell = driver.putCardInHand(opponent, "Test Investigate")
        driver.giveMana(opponent, Color.BLUE, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, spell).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.clues(opponent) shouldBe 1
        driver.clues(me) shouldBe 0
    }

    test("the next turn's first investigate triggers it again") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Erdwal Illuminator")

        driver.castAndResolve(me, "Test Investigate")
        driver.clues(me) shouldBe 2

        // On to the opponent's turn: the per-turn marker cleared at end of turn.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe driver.getOpponent(me)
        val spell = driver.putCardInHand(me, "Test Investigate")
        driver.giveMana(me, Color.BLUE, 1)
        if (driver.state.priorityPlayerId != me) driver.passPriority(driver.state.priorityPlayerId!!)
        driver.castSpell(me, spell).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.clues(me) shouldBe 4
    }
})
