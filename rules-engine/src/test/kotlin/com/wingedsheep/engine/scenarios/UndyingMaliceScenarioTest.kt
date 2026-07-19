package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.InnistradCrimsonVowSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Undying Malice (VOW).
 *
 * "Until end of turn, target creature gains 'When this creature dies, return it to the
 * battlefield tapped under its owner's control with a +1/+1 counter on it.'"
 *
 * Not a new engine keyword — the granted clause composes a SELF-bound dies trigger
 * (battlefield → graveyard) that moves the card graveyard → battlefield tapped and then
 * adds a +1/+1 counter. The graveyard → battlefield return keeps the same entity id, so the
 * follow-up AddCounters lands on the returned permanent. These tests prove the granted
 * self-trigger rides the engine's trigger pipeline and returns the creature exactly once.
 */
class UndyingMaliceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + InnistradCrimsonVowSet.cards)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("granted creature dies — returns tapped with a +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A 2/2 red Goblin Guide is our target — nonblack, so Doom Blade can kill it.
        val goblin = driver.putCreatureOnBattlefield(you, "Goblin Guide")

        val malice = driver.putCardInHand(you, "Undying Malice")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpell(you, malice, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass()  // resolve Undying Malice — grant applied for the turn

        // Kill the granted creature.
        val doomBlade = driver.putCardInHand(you, "Doom Blade")
        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, doomBlade, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass()  // resolve Doom Blade — Goblin dies, granted "when this dies" trigger goes on stack
        driver.bothPass()  // resolve the granted return trigger

        // Same entity id is back on the battlefield…
        driver.state.getBattlefield().contains(goblin) shouldBe true
        // …tapped…
        driver.isTapped(goblin) shouldBe true
        // …with exactly one +1/+1 counter.
        plusOneCounters(driver, goblin) shouldBe 1
    }

    test("returns exactly once — the returned creature is a fresh object without the grant (CR 400.7)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCreatureOnBattlefield(you, "Goblin Guide")

        val malice = driver.putCardInHand(you, "Undying Malice")
        driver.giveMana(you, Color.BLACK, 1)
        driver.castSpell(you, malice, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass()

        // While the grant is live it rides the creature.
        driver.state.grantedTriggeredAbilities.any { it.entityId == goblin } shouldBe true

        val doomBlade = driver.putCardInHand(you, "Doom Blade")
        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, doomBlade, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass()  // Doom Blade resolves — Goblin dies
        driver.bothPass()  // return trigger resolves — Goblin comes back

        driver.state.getBattlefield().contains(goblin) shouldBe true
        // CR 400.7: the returned Goblin is a new object — the until-end-of-turn grant did not
        // follow it, so a second death would NOT return it again (no infinite loop).
        driver.state.grantedTriggeredAbilities.any { it.entityId == goblin } shouldBe false
    }
})
