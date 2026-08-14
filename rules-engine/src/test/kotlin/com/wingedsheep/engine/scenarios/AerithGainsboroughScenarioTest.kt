package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.AerithGainsborough
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Aerith Gainsborough — {2}{W} Legendary Creature — Human Cleric 2/2
 *   Lifelink
 *   Whenever you gain life, put a +1/+1 counter on Aerith Gainsborough.
 *   When Aerith Gainsborough dies, put X +1/+1 counters on each legendary creature you control,
 *   where X is the number of +1/+1 counters on Aerith Gainsborough.
 *
 * Covers: the life-gain → counter trigger (driven by a real lifelink combat life-gain event), and
 * the dies trigger distributing last-known-information counters to legendary creatures you control
 * (non-legendary and opponent-controlled creatures excluded).
 */
class AerithGainsboroughScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AerithGainsborough))
        return driver
    }

    fun addCounters(driver: GameTestDriver, entityId: EntityId, type: CounterType, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        }
        driver.replaceState(newState)
    }

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("gaining life via lifelink combat puts a +1/+1 counter on Aerith") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val aerith = driver.putCreatureOnBattlefield(active, "Aerith Gainsborough")
        driver.removeSummoningSickness(aerith)

        plusOneCounters(driver, aerith) shouldBe 0
        val lifeBefore = driver.getLifeTotal(active)

        // Attack unblocked: lifelink gains 2 life from the 2 combat damage dealt to the opponent,
        // and Aerith survives so the "whenever you gain life" counter trigger has a home (a blocker
        // trade would kill Aerith first — per ruling that fizzles the counter).
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(aerith), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is com.wingedsheep.engine.core.CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }
        // Resolve the "whenever you gain life" trigger that the lifelink life-gain queued.
        driver.bothPass()

        // Lifelink: Aerith dealt 2 combat damage, so its controller gained 2 life — one event.
        driver.getLifeTotal(active) shouldBe (lifeBefore + 2)
        // That single life-gain event triggers the counter ability exactly once.
        plusOneCounters(driver, aerith) shouldBe 1
    }

    test("when Aerith dies, each legendary creature you control gets X +1/+1 counters (X = Aerith's +1/+1 counters)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val aerith = driver.putCreatureOnBattlefield(active, "Aerith Gainsborough")
        // Aerith has accumulated three +1/+1 counters (e.g. from life gain).
        addCounters(driver, aerith, CounterType.PLUS_ONE_PLUS_ONE, 3)

        // A legendary creature you control should receive the counters (distinct name, so no
        // legend-rule conflict with Aerith).
        val myLegend = driver.putCreatureOnBattlefield(active, "Test Hasty Prospector")
        // A non-legendary creature you control must NOT receive counters.
        val myNonLegend = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        // An opponent's legendary creature must NOT receive counters (legendary "you control").
        val theirLegend = driver.putCreatureOnBattlefield(opponent, "Ghalta, Primal Hunger")

        // Kill Aerith for real (Doom Blade -> destroy) so the dies trigger fires with last-known
        // counter information captured on the ZoneChangeEvent.
        val doomBlade = driver.putCardInHand(active, "Doom Blade")
        driver.giveMana(active, com.wingedsheep.sdk.core.Color.BLACK, 2)
        driver.castSpell(active, doomBlade, targets = listOf(aerith)).isSuccess shouldBe true
        driver.bothPass() // resolve Doom Blade -> Aerith is destroyed, queuing its dies trigger
        driver.state.getBattlefield().contains(aerith) shouldBe false
        driver.bothPass() // resolve Aerith's dies trigger

        // X = 3 counters were on Aerith; each legendary creature you control gains 3.
        plusOneCounters(driver, myLegend) shouldBe 3
        // Non-legendary creature unaffected.
        plusOneCounters(driver, myNonLegend) shouldBe 0
        // Opponent's legendary creature unaffected.
        plusOneCounters(driver, theirLegend) shouldBe 0
    }
})
