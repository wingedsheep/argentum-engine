package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.FeignDeath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feign Death (AFR #103) — {B} Instant.
 *
 *   Until end of turn, target creature gains "When this creature dies, return it to the
 *   battlefield tapped under its owner's control with a +1/+1 counter on it."
 *
 * The granted dies trigger returns the card graveyard → battlefield with the +1/+1 counter as an
 * entry counter on the move. These tests prove the entry counter lands on a graveyard return
 * (same entity id), the creature comes back tapped, and the grant does not follow the new object.
 */
class FeignDeathScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FeignDeath))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("granted creature dies — returns tapped with exactly one +1/+1 counter, grant gone") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCreatureOnBattlefield(you, "Goblin Guide")

        val feign = driver.putCardInHand(you, "Feign Death")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpell(you, feign, listOf(goblin)).outcome shouldBe Outcome.Done
        driver.bothPass()  // resolve Feign Death — grant applied for the turn

        driver.state.grantedTriggeredAbilities.any { it.entityId == goblin } shouldBe true

        val doomBlade = driver.putCardInHand(you, "Doom Blade")
        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, doomBlade, listOf(goblin)).outcome shouldBe Outcome.Done
        driver.bothPass()  // Doom Blade resolves — Goblin dies, granted trigger goes on the stack
        driver.bothPass()  // return trigger resolves

        driver.state.getBattlefield().contains(goblin) shouldBe true
        driver.isTapped(goblin) shouldBe true
        plusOneCounters(driver, goblin) shouldBe 1
        // CR 400.7: the returned creature is a new object — the grant did not follow it.
        driver.state.grantedTriggeredAbilities.any { it.entityId == goblin } shouldBe false
    }
})
