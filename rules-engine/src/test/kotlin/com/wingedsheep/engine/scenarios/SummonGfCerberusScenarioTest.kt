package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.SummonGfCerberus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Summon: G.F. Cerberus — {2}{R}{R} Enchantment Creature — Saga Dog, 3/3 (FIN).
 *
 *   I   — Surveil 1.
 *   II  — Double — When you next cast an instant or sorcery spell this turn, copy it.
 *   III — Triple — When you next cast an instant or sorcery spell this turn, copy it twice.
 *
 * Generic saga-creature machinery (lore accrual, chapter triggers, sacrifice after the final
 * chapter) is covered by CreatureSagaTest, and the copy-your-next-spell rider itself by
 * EtherScenarioTest; this pins Cerberus's chapters II and III producing one / two copies of the
 * next instant cast, doubling / tripling Shock's damage.
 */
class SummonGfCerberusScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.loreCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SummonGfCerberus))
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.castCerberus(me: EntityId): EntityId {
        val spell = putCardInHand(me, "Summon: G.F. Cerberus")
        giveColorlessMana(me, 2)
        giveMana(me, Color.RED, 2)
        castSpell(me, spell)
        return spell
    }

    fun GameTestDriver.advanceUntil(maxSteps: Int = 2000, predicate: GameTestDriver.() -> Boolean) {
        var guard = 0
        while (guard++ < maxSteps && !predicate()) {
            val pd = state.pendingDecision
            when {
                pd != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
        }
    }

    /** Cast Shock at [opp], resolving every copy (keeping [opp] as the target) and the original. */
    fun GameTestDriver.castShockAt(me: EntityId, opp: EntityId) {
        val shock = putCardInHand(me, "Shock")
        giveMana(me, Color.RED, 1)
        castSpell(me, shock, listOf(opp)).isSuccess shouldBe true
        var guard = 0
        while (stackSize > 0 && guard++ < 30) {
            if (state.pendingDecision is ChooseTargetsDecision) submitTargetSelection(me, listOf(opp))
            else bothPass()
        }
    }

    test("enters as a 3/3 Saga Dog creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castCerberus(me)
        driver.resolveAll()

        val cerberus = driver.findPermanent(me, "Summon: G.F. Cerberus")!!
        val projected = projector.project(driver.state)
        projected.isCreature(cerberus) shouldBe true
        projected.hasType(cerberus, "Saga") shouldBe true
        projected.getPower(cerberus) shouldBe 3
        projected.getToughness(cerberus) shouldBe 3
    }

    test("chapter II — copies the next instant once, doubling Shock's damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.player2
        driver.castCerberus(me)
        driver.resolveAll()

        // Advance to the turn chapter II resolves (second lore counter).
        driver.advanceUntil {
            val s = findPermanent(me, "Summon: G.F. Cerberus") ?: return@advanceUntil true
            loreCounters(s) >= 2
        }
        driver.resolveAll()

        val before = driver.getLifeTotal(opp)
        driver.castShockAt(me, opp)
        // 2 (original) + 2 (one copy) = 4.
        driver.getLifeTotal(opp) shouldBe before - 4
    }

    test("chapter III — copies the next instant twice, tripling Shock's damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.player2
        driver.castCerberus(me)
        driver.resolveAll()

        // Advance to the turn chapter III resolves (third lore counter). Chapter II's rider was
        // cast-free that turn and expired, so only chapter III's "copy twice" rider is live now.
        driver.advanceUntil {
            val s = findPermanent(me, "Summon: G.F. Cerberus") ?: return@advanceUntil false
            loreCounters(s) >= 3
        }
        driver.resolveAll()

        val before = driver.getLifeTotal(opp)
        driver.castShockAt(me, opp)
        // 2 (original) + 2 + 2 (two copies) = 6.
        driver.getLifeTotal(opp) shouldBe before - 6
    }
})
