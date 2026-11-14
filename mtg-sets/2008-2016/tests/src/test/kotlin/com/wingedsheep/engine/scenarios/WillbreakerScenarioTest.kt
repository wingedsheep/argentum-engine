package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.IcyManipulator
import com.wingedsheep.mtg.sets.definitions.ori.cards.Willbreaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Willbreaker (ORI #84) — "Whenever a creature an opponent controls becomes the target of a spell
 * or ability you control, gain control of that creature for as long as you control this creature."
 *
 * Covers both kinds of targeting object (a spell and an activated ability), the two filters that
 * keep it quiet (your own creature; an opponent's spell), and the duration ending once Willbreaker
 * leaves the battlefield.
 */
class WillbreakerScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Willbreaker + IcyManipulator)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("targeting an opponent's creature with your spell steals it") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Willbreaker")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(bears)).outcome shouldBe Outcome.Done
        resolveStack(d)

        withClue("the trigger hands the targeted creature to Willbreaker's controller") {
            projector.project(d.state).getController(bears) shouldBe d.player1
        }
        withClue("the spell still resolves on its target") {
            projector.project(d.state).getPower(bears) shouldBe 5
        }
    }

    test("an activated ability you control counts too") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Willbreaker")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val manipulator = d.putPermanentOnBattlefield(d.player1, "Icy Manipulator")
        d.giveColorlessMana(d.player1, 1)

        d.submit(
            ActivateAbility(
                d.player1,
                manipulator,
                IcyManipulator.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        ).outcome shouldBe Outcome.Done
        resolveStack(d)

        projector.project(d.state).getController(bears) shouldBe d.player1
    }

    test("an opponent targeting their own creature does not trigger it") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Willbreaker")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.passPriority(d.player1)
        val growth = d.putCardInHand(d.player2, "Giant Growth")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castSpell(d.player2, growth, listOf(bears)).outcome shouldBe Outcome.Done
        resolveStack(d)

        projector.project(d.state).getController(bears) shouldBe d.player2
    }

    test("targeting your own creature changes nothing") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Willbreaker")
        val ownBears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(ownBears)).outcome shouldBe Outcome.Done
        resolveStack(d)

        projector.project(d.state).getController(ownBears) shouldBe d.player1
        projector.project(d.state).getPower(ownBears) shouldBe 5
    }

    test("control returns once Willbreaker leaves the battlefield") {
        val d = driver()
        val willbreaker = d.putCreatureOnBattlefield(d.player1, "Willbreaker")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(bears)).outcome shouldBe Outcome.Done
        resolveStack(d)
        projector.project(d.state).getController(bears) shouldBe d.player1

        d.passPriority(d.player1)
        val bolt = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        d.castSpell(d.player2, bolt, listOf(willbreaker)).outcome shouldBe Outcome.Done
        resolveStack(d)

        withClue("Willbreaker died to the Bolt") {
            d.getGraveyardCardNames(d.player1) shouldContain "Willbreaker"
        }
        withClue("the steal lasted only as long as its controller controlled Willbreaker") {
            projector.project(d.state).getController(bears) shouldBe d.player2
        }
    }
})
