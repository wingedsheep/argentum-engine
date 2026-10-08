package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "You gain hexproof / shroud until end of turn" — `Effects.GrantHexproof(Controller)` /
 * `Effects.GrantShroud(Controller)` on a player (CR 702.11, CR 702.18).
 *
 * Regression: the executor built the player component in a `when` whose branches widened to
 * `Component`, and `ComponentContainer.with` keys by its reified type parameter — so the grant was
 * stored under the `Component` key, invisible to `ControllerHexproof` / `ControllerShroud` and to
 * end-of-turn cleanup. The player was never actually protected. These tests pin the grant through
 * the targeting rules a player sees, and its expiry.
 */
class PlayerEvasionKeywordGrantTest : FunSpec({

    val veil = card("Test Veil of Hexproof") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell { effect = Effects.GrantHexproof(EffectTarget.Controller) }
    }

    val cloak = card("Test Cloak of Shroud") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell { effect = Effects.GrantShroud(EffectTarget.Controller) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(veil, cloak))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("you gain hexproof: an opponent can't target you, and it ends at end of turn") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        val spell = driver.putCardInHand(p1, "Test Veil of Hexproof")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.castSpell(p1, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("the player carries the hexproof component under its own type") {
            driver.state.getEntity(p1)?.get<PlayerHexproofComponent>() shouldNotBe null
        }

        driver.passPriority(p1)
        val bolt = driver.putCardInHand(p2, "Lightning Bolt")
        driver.giveMana(p2, Color.RED, 1)
        withClue("an opponent's Lightning Bolt can't target a hexproof player") {
            driver.castSpellWithTargets(p2, bolt, listOf(ChosenTarget.Player(p1))).error shouldNotBe null
        }

        driver.passPriorityUntil(Step.UPKEEP, activePlayer = p2)
        withClue("hexproof granted until end of turn is gone next turn") {
            driver.state.getEntity(p1)?.get<PlayerHexproofComponent>() shouldBe null
        }
    }

    test("you gain shroud: you can't target yourself either") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!

        val spell = driver.putCardInHand(p1, "Test Cloak of Shroud")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.castSpell(p1, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getEntity(p1)?.get<PlayerShroudComponent>() shouldNotBe null

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        withClue("a shrouded player can't be the target of their own spell") {
            driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p1))).error shouldNotBe null
        }
    }
})
