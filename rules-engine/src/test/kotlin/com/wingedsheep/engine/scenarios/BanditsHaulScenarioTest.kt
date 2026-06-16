package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BanditsHaul
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bandit's Haul — {3} Artifact
 *
 * "Whenever you commit a crime, put a loot counter on this artifact. This ability triggers only
 * once each turn."
 * "{T}: Add one mana of any color."
 * "{2}, {T}, Remove two loot counters from this artifact: Draw a card."
 *
 * Verifies: a crime adds a loot counter (once per turn); two crimes across two turns accumulate two
 * counters; the draw ability spends two loot counters to draw a card.
 */
class BanditsHaulScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.lootCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOOT) ?: 0

    fun GameTestDriver.commitCrime(caster: EntityId, opponent: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, targets = listOf(opponent))
        bothPass() // resolve Bolt -> commit-crime -> loot trigger on stack
        bothPass() // resolve loot trigger
    }

    test("committing a crime puts a loot counter, but only once per turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val haul = driver.putPermanentOnBattlefield(me, "Bandit's Haul")

        driver.commitCrime(me, opp)
        driver.lootCounters(haul) shouldBe 1

        // A second crime the same turn must NOT add another counter (once per turn).
        driver.commitCrime(me, opp)
        driver.lootCounters(haul) shouldBe 1
    }

    test("draw ability removes two loot counters to draw a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val haul = driver.putPermanentOnBattlefield(me, "Bandit's Haul")

        // First crime this turn -> 1 loot counter.
        driver.commitCrime(me, opp)
        driver.lootCounters(haul) shouldBe 1

        // Advance two full turns (my turn -> opponent's turn -> my next turn) so the once-per-turn
        // gate resets, then commit another crime on my own turn -> 2 loot counters.
        repeat(2) {
            driver.passPriorityUntil(Step.END)
            driver.bothPass()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        driver.activePlayer shouldBe me
        driver.commitCrime(me, opp)
        driver.lootCounters(haul) shouldBe 2

        val handBefore = driver.getHandSize(me)
        val abilityId = BanditsHaul.activatedAbilities[1].id // the {2},{T},remove-2-loot draw ability
        driver.giveMana(me, Color.BLACK, 2)
        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = haul, abilityId = abilityId)
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the draw

        driver.getHandSize(me) shouldBe handBefore + 1
        driver.lootCounters(haul) shouldBe 0
    }
})
