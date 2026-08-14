package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression: an "other target" slot (CR 601.2c) whose every legal target was already spent on an
 * earlier slot used to crash the AI.
 *
 * The enumerator cannot know which target the earlier slot will take, so it offers the same
 * creature for both — and [TargetSelection.fillHeuristically] then had nothing left for the second,
 * where its `?: available.first()` fallback could only ever run on an empty list. Found by fuzzing
 * self-play with random decks; it takes the AI's whole priority window down with it, which in a
 * served game means the AI simply stops playing.
 */
class OtherTargetFillAiTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply { register(TestCards.all) }

    /** A game in the AI's own main phase, with [mana] of [color] available. */
    fun mainPhase(driver: GameTestDriver, color: Color, mana: Int): EntityId {
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(driver.player1, color, mana)
        return driver.player1
    }

    fun castOf(registry: CardRegistry, driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        GameSimulator(registry).getLegalActions(driver.state, player)
            .first { (it.action as? CastSpell)?.cardId == cardId }

    test("an optional other-target slot with nothing left is simply left empty") {
        val registry = registry()
        val driver = GameTestDriver()
        val player = mainPhase(driver, Color.WHITE, 2)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        // "Target creature gets +2/+2. Up to one *other* target creature gets +1/+1." — with one
        // creature on the board the second slot has nothing left once the first has taken it.
        val mettle = driver.putCardInHand(player, "Mabel's Mettle")

        val filled = TargetSelection.fillHeuristically(
            driver.state, castOf(registry, driver, player, mettle), player, fillPartialRequirements = false
        )

        (filled as CastSpell).targets shouldBe listOf(ChosenTarget.Permanent(bears))
        // A flat target list may leave trailing optional slots empty, so the engine takes this.
        driver.submitSuccess(filled)
    }

    test("a mandatory other-target slot with nothing left leaves the action unfilled") {
        val registry = registry()
        val driver = GameTestDriver()
        val player = mainPhase(driver, Color.GREEN, 6)
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        // "Target creature you control deals damage equal to its power to each of two *other*
        // target creatures" — three slots, only two creatures in the game.
        val betrayal = driver.putCardInHand(player, "Betrayal at the Vault")

        val action = castOf(registry, driver, player, betrayal)
        val filled = TargetSelection.fillHeuristically(
            driver.state, action, player, fillPartialRequirements = false
        )

        // No legal target list exists, so the AI hands the action back untouched for its own
        // simulation — or the engine — to reject, rather than inventing an illegal one.
        (filled as CastSpell).targets shouldBe emptyList()
        driver.submit(filled).isSuccess shouldBe false
    }

    test("the AI survives a priority window holding such a spell") {
        val registry = registry()
        val driver = GameTestDriver()
        val player = mainPhase(driver, Color.WHITE, 2)
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCardInHand(player, "Mabel's Mettle")

        val ai = AIPlayer.create(registry, player, AiProfile.PRODUCTION)

        shouldNotThrowAny { ai.chooseAction(driver.state) }
    }
})
