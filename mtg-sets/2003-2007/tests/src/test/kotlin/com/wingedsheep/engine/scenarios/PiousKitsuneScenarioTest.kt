package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.EightAndAHalfTails
import com.wingedsheep.mtg.sets.definitions.chk.cards.PiousKitsune
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pious Kitsune (CHK #38) — "At the beginning of your upkeep, put a devotion counter on this
 * creature. Then if a creature named Eight-and-a-Half-Tails is on the battlefield, you gain 1 life
 * for each devotion counter on this creature. {T}, Remove a devotion counter from this creature:
 * You gain 1 life."
 */
class PiousKitsuneScenarioTest : FunSpec({

    val pray = PiousKitsune.activatedAbilities[0].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PiousKitsune + EightAndAHalfTails)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.devotion(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.DEVOTION) ?: 0

    fun GameTestDriver.giveDevotion(id: EntityId, n: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.DEVOTION, n))
        })
    }

    /** Pass through the opponent's turn to player 1's next upkeep and resolve the trigger. */
    fun GameTestDriver.toMyUpkeepAndResolve() {
        passPriorityUntil(Step.UPKEEP)          // opponent's upkeep
        passPriorityUntil(Step.DRAW)
        passPriorityUntil(Step.UPKEEP)          // player 1's upkeep
        activePlayer shouldBe player1
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("upkeep adds a devotion counter and gains no life without Eight-and-a-Half-Tails") {
        val d = driver()
        val kitsune = d.putCreatureOnBattlefield(d.player1, "Pious Kitsune")
        d.giveDevotion(kitsune, 2)

        d.toMyUpkeepAndResolve()

        d.devotion(kitsune) shouldBe 3
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("with an opponent's Eight-and-a-Half-Tails, gain life equal to devotion including the new counter") {
        val d = driver()
        val kitsune = d.putCreatureOnBattlefield(d.player1, "Pious Kitsune")
        d.putCreatureOnBattlefield(d.getOpponent(d.player1), "Eight-and-a-Half-Tails")
        d.giveDevotion(kitsune, 2)

        d.toMyUpkeepAndResolve()

        d.devotion(kitsune) shouldBe 3
        withClue("the counter is placed before the count is read") {
            d.getLifeTotal(d.player1) shouldBe 23
        }
    }

    test("tap and remove a devotion counter to gain 1 life") {
        val d = driver()
        val kitsune = d.putCreatureOnBattlefield(d.player1, "Pious Kitsune")
        d.removeSummoningSickness(kitsune)
        d.giveDevotion(kitsune, 1)

        d.submit(ActivateAbility(d.player1, kitsune, pray)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getLifeTotal(d.player1) shouldBe 21
        d.devotion(kitsune) shouldBe 0
        d.isTapped(kitsune) shouldBe true
    }

    test("the activated ability can't be activated without a devotion counter") {
        val d = driver()
        val kitsune = d.putCreatureOnBattlefield(d.player1, "Pious Kitsune")
        d.removeSummoningSickness(kitsune)

        d.submit(ActivateAbility(d.player1, kitsune, pray)).outcome shouldNotBe Outcome.Done
        d.isTapped(kitsune) shouldBe false
    }
})
