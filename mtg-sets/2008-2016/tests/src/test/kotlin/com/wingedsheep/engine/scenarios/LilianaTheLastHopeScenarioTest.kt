package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.LilianaTheLastHope
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Liliana, the Last Hope (EMN #93): the +1's -2/-1 outlasts the opponent's turn and ends as her
 * controller's next turn begins; the −7 emblem's X is counted as its trigger resolves — two with no
 * Zombies (ruling), and growing with the tokens it already made — and only on its owner's end step.
 */
class LilianaTheLastHopeScenarioTest : FunSpec({
    val zombie = card("Test Zombie") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(LilianaTheLastHope, zombie))
        it.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.putLiliana(player: EntityId, loyalty: Int): EntityId =
        putPermanentOnBattlefield(player, "Liliana, the Last Hope").also {
            addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        }
    fun GameTestDriver.drain() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }
    fun GameTestDriver.zombieTokens(player: EntityId): Int =
        getPermanents(player).count { id ->
            val entity = state.getEntity(id)
            entity?.get<TokenComponent>() != null &&
                entity.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Zombie" } == true
        }
    fun GameTestDriver.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
    val abilities = LilianaTheLastHope.script.activatedAbilities

    test("+1 gives -2/-1 through the opponent's turn and wears off on Liliana's controller's next turn") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val liliana = d.putLiliana(me, 3)
        val bears = d.putPermanentOnBattlefield(opp, "Grizzly Bears")

        d.submitSuccess(ActivateAbility(me, liliana, abilities[0].id,
            targets = listOf(ChosenTarget.Permanent(bears))))
        d.drain()
        d.loyalty(liliana) shouldBe 4
        d.state.projectedState.getPower(bears) shouldBe 0
        d.state.projectedState.getToughness(bears) shouldBe 1

        d.passPriorityUntil(Step.END, opp)
        d.state.projectedState.getPower(bears) shouldBe 0
        d.state.projectedState.getToughness(bears) shouldBe 1

        d.passPriorityUntil(Step.UPKEEP, me)
        d.state.projectedState.getPower(bears) shouldBe 2
        d.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("+1 can be activated with no target just for the loyalty") {
        val d = driver()
        val me = d.activePlayer!!
        val liliana = d.putLiliana(me, 3)

        d.submitSuccess(ActivateAbility(me, liliana, abilities[0].id, targets = emptyList()))
        d.drain()
        d.loyalty(liliana) shouldBe 4
    }

    test("emblem makes two Zombies with none in play, then two plus the Zombies already there") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val liliana = d.putLiliana(me, 7)

        d.submitSuccess(ActivateAbility(me, liliana, abilities[2].id))
        d.drain()
        d.getGraveyardCardNames(me).contains("Liliana, the Last Hope") shouldBe true

        d.passPriorityUntil(Step.END, me)
        d.drain()
        d.zombieTokens(me) shouldBe 2

        // The emblem is "your end step" only: the opponent's end step makes nothing.
        d.passPriorityUntil(Step.END, opp)
        d.drain()
        d.zombieTokens(me) shouldBe 2

        // Two plus the two Zombies already in play.
        d.passPriorityUntil(Step.END, me)
        d.drain()
        d.zombieTokens(me) shouldBe 6
    }

    test("emblem counts nontoken Zombies too") {
        val d = driver()
        val me = d.activePlayer!!
        val liliana = d.putLiliana(me, 7)
        d.putPermanentOnBattlefield(me, "Test Zombie")

        d.submitSuccess(ActivateAbility(me, liliana, abilities[2].id))
        d.drain()

        d.passPriorityUntil(Step.END, me)
        d.drain()
        d.zombieTokens(me) shouldBe 3
    }
})
