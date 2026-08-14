package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.ConstructACosmicCube
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Construct a Cosmic Cube (MSH) — "Whenever you draw your second card each turn, create a 2/1 black
 * Villain creature token with menace and put a plan counter on this enchantment."
 *
 * Playtesting reported it not firing off a Jalum Tome activation. The clause counts draws *per turn*,
 * so the first draw of a turn is correctly a no-op — this pins both halves of that: silent on the
 * first draw, firing on the second.
 */
class ConstructACosmicCubeScenarioTest : FunSpec({

    val drawOne = card("Draw One Test Spell") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(ConstructACosmicCube)
        registerCard(drawOne)
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.planCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLAN) ?: 0

    fun GameTestDriver.settle() {
        repeat(12) {
            if (state.pendingDecision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
    }

    fun GameTestDriver.drawOneCard() {
        val spell = putCardInHand(player1, "Draw One Test Spell")
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, spell).isSuccess shouldBe true
        settle()
    }

    test("silent on the first draw of the turn, fires on the second") {
        val d = driver()
        val cube = d.putPermanentOnBattlefield(d.player1, "Construct a Cosmic Cube")

        d.drawOneCard()
        withClue("first draw of the turn must not trigger") {
            d.planCounters(cube) shouldBe 0
        }

        d.drawOneCard()
        withClue("second draw of the turn must trigger") {
            d.planCounters(cube) shouldBe 1
        }
    }
})
