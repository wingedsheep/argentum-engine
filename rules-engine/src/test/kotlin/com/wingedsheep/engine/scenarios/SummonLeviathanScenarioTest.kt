package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.SummonLeviathan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Summon: Leviathan — {4}{U}{U} Enchantment Creature — Saga Leviathan, 6/6, Ward {2} (FIN).
 *
 *   I       — Return each creature that isn't a Kraken, Leviathan, Merfolk, Octopus, or Serpent to
 *             its owner's hand.
 *   II, III — Until end of turn, whenever a [sea creature] attacks, draw a card.
 *
 * Generic saga-creature machinery is covered by CreatureSagaTest and the end-of-turn delayed
 * attack-trigger primitive by SummonFenrirScenarioTest; this pins chapter I's *filtered* mass
 * bounce — non-sea creatures are returned while sea creatures (and Leviathan itself, a Leviathan)
 * stay put.
 */
class SummonLeviathanScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SummonLeviathan))
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.castLeviathan(me: EntityId): EntityId {
        val spell = putCardInHand(me, "Summon: Leviathan")
        giveColorlessMana(me, 4)
        giveMana(me, Color.BLUE, 2)
        castSpell(me, spell)
        return spell
    }

    test("enters as a 6/6 Saga Leviathan creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castLeviathan(me)
        driver.resolveAll()

        val leviathan = driver.findPermanent(me, "Summon: Leviathan")!!
        val projected = projector.project(driver.state)
        projected.isCreature(leviathan) shouldBe true
        projected.hasType(leviathan, "Saga") shouldBe true
        projected.getPower(leviathan) shouldBe 6
        projected.getToughness(leviathan) shouldBe 6
    }

    test("chapter I — returns non-sea creatures but not sea creatures or Leviathan itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // A non-sea creature (Centaur) and a sea creature (Island Walker is a Merfolk).
        driver.putPermanentOnBattlefield(me, "Centaur Courser")
        driver.putPermanentOnBattlefield(me, "Island Walker")
        val handBefore = driver.getHandSize(me)

        driver.castLeviathan(me)
        driver.resolveAll()

        // The Centaur was returned to its owner's hand; the Merfolk and the Leviathan stayed.
        driver.findPermanent(me, "Centaur Courser") shouldBe null
        (driver.findPermanent(me, "Island Walker") != null) shouldBe true
        (driver.findPermanent(me, "Summon: Leviathan") != null) shouldBe true
        // Leviathan is cast hand-neutral (drawn into hand, then cast), so the only net change is
        // the bounced Centaur returning to hand: handBefore + 1.
        driver.getHandSize(me) shouldBe handBefore + 1
    }
})
