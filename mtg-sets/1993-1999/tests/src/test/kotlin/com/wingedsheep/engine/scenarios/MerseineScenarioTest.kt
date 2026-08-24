package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.Merseine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Merseine (Fallen Empires).
 *
 * Two clauses need the engine to do something new: the cost is read off the *enchanted* permanent
 * rather than the Aura, and the ability belongs to the trapped creature's controller rather than to
 * the Aura's. A Merseine on an opponent's creature is where the two diverge, so that is the board
 * these tests use.
 */
class MerseineScenarioTest : FunSpec({

    val abilityId = Merseine.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Merseine)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    test("the trapped creature's controller pays that creature's cost to remove a net counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Elvish Warrior costs {G}{G}: that is the ransom, not Merseine's own {2}{U}{U}.
        val warrior = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")
        val merseine = driver.putPermanentOnBattlefield(alice, "Merseine")
        attach(driver, merseine, warrior)

        // The driver puts a permanent straight onto the battlefield, which bypasses enters-with
        // replacements, so the three net counters are set by hand here. The replacement itself is
        // the same one Triskelion and Braided Net use and is covered by their tests.
        driver.replaceState(
            driver.state.updateEntity(merseine) { c ->
                c.with(CountersComponent(mapOf(CounterType.NET to 3)))
            }
        )

        driver.passPriority(alice)
        driver.giveMana(bob, Color.GREEN, 2)
        driver.submitSuccess(
            ActivateAbility(playerId = bob, sourceId = merseine, abilityId = abilityId)
        )
        driver.bothPass()

        withClue("Bob bought one counter off with {G}{G}") {
            driver.state.getEntity(merseine)?.get<CountersComponent>()?.getCount(CounterType.NET) shouldBe 2
        }
    }

    test("the Aura's controller may not activate it when someone else controls the creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warrior = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")
        val merseine = driver.putPermanentOnBattlefield(alice, "Merseine")
        attach(driver, merseine, warrior)

        driver.giveMana(alice, Color.GREEN, 2)
        driver.submit(
            ActivateAbility(playerId = alice, sourceId = merseine, abilityId = abilityId)
        ).isSuccess shouldBe false
    }
})
