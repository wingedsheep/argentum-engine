package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.StromkirkBloodthief
import com.wingedsheep.mtg.sets.definitions.mid.cards.VampireSocialite
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vampire Socialite — {B}{R} Vampire Noble 2/2, Menace.
 *
 * "When this creature enters, if an opponent lost life this turn, put a +1/+1 counter on each other
 * Vampire you control. As long as an opponent lost life this turn, each other Vampire you control
 * enters with an additional +1/+1 counter on it."
 *
 * Pins both halves of the opponent-life-loss gate: the ETB counters the *other* Vampires only, and
 * the conditional enters-with replacement adds a counter to a later Vampire but not to the Socialite.
 */
class VampireSocialiteScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(VampireSocialite, StromkirkBloodthief))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        return d
    }

    fun GameTestDriver.settle(maxSteps: Int = 20) {
        var guard = 0
        while (stackSize > 0 && guard++ < maxSteps) bothPass()
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castSocialite(you: EntityId): EntityId {
        val socialite = putCardInHand(you, "Vampire Socialite")
        giveMana(you, Color.BLACK, 1)
        giveMana(you, Color.RED, 1)
        castSpell(you, socialite).outcome shouldBe Outcome.Done
        settle()
        return socialite
    }

    test("opponent lost life: ETB counters other Vampires, later Vampires enter with a counter") {
        val d = driver()
        val you = d.player1
        val opp = d.player2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val earlyVampire = d.putCreatureOnBattlefield(you, "Stromkirk Bloodthief")

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(opp)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(opp) shouldBe 17

        val socialite = d.castSocialite(you)
        d.plusCounters(earlyVampire) shouldBe 1
        d.plusCounters(socialite) shouldBe 0

        val lateVampire = d.putCardInHand(you, "Stromkirk Bloodthief")
        d.giveMana(you, Color.BLACK, 3)
        d.castSpell(you, lateVampire).outcome shouldBe Outcome.Done
        d.settle()
        d.plusCounters(lateVampire) shouldBe 1
        d.plusCounters(earlyVampire) shouldBe 1
    }

    test("no opponent life loss: no ETB counters and no additional counter on entry") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val earlyVampire = d.putCreatureOnBattlefield(you, "Stromkirk Bloodthief")
        val socialite = d.castSocialite(you)
        d.plusCounters(earlyVampire) shouldBe 0
        d.plusCounters(socialite) shouldBe 0

        val lateVampire = d.putCardInHand(you, "Stromkirk Bloodthief")
        d.giveMana(you, Color.BLACK, 3)
        d.castSpell(you, lateVampire).outcome shouldBe Outcome.Done
        d.settle()
        d.plusCounters(lateVampire) shouldBe 0
    }
})
