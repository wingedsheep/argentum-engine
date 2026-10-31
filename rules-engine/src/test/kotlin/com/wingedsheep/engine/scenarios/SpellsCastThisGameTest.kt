package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmount.SpellsCastThisGame` — how many spells a player has cast this game.
 *
 * Rules pinned: a spell counts once cast whether or not it resolves (a countered spell was still
 * cast); the count is per player and never resets at a turn boundary, unlike the per-turn spell
 * counts; and a multi-player scope sums its players.
 */
class SpellsCastThisGameTest : FunSpec({

    val amounts = PredicateEvaluator(cardRegistry = null).amounts

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun GameTestDriver.castThisGame(viewer: EntityId, player: Player = Player.You): Int =
        amounts.evaluate(state, DynamicAmounts.spellsCastThisGame(player), EffectContext(sourceId = null, controllerId = viewer))

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, listOf(target)).error shouldBe null
    }

    test("starts at zero and counts each cast for its caster only") {
        val (driver, you, opponent) = setup()
        driver.castThisGame(you) shouldBe 0

        driver.bolt(you, opponent)
        driver.bothPass()
        driver.bolt(you, opponent)
        driver.bothPass()

        driver.castThisGame(you) shouldBe 2
        withClue("the opponent cast nothing") { driver.castThisGame(opponent) shouldBe 0 }
        withClue("Player.Each sums both players") { driver.castThisGame(you, Player.Each) shouldBe 2 }
    }

    test("a countered spell was still cast, and so was the counterspell") {
        val (driver, you, opponent) = setup()
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.bolt(you, opponent)
        val boltOnStack = driver.state.stack.last()
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(boltOnStack)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        withClue("the Bolt was countered") { driver.getLifeTotal(opponent) shouldBe 20 }
        driver.castThisGame(you) shouldBe 1
        driver.castThisGame(opponent) shouldBe 1
    }

    test("the count survives the turn boundary that clears the per-turn counts") {
        val (driver, you, opponent) = setup()
        driver.bolt(you, opponent)
        driver.bothPass()

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        withClue("this turn's count reset") {
            amounts.evaluate(driver.state, DynamicAmounts.spellsCastThisTurn(), EffectContext(sourceId = null, controllerId = you)) shouldBe 0
        }
        driver.castThisGame(you) shouldBe 1
    }
})
