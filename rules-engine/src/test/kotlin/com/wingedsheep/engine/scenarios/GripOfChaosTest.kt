package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetReselectedEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for ReselectTargetRandomlyExecutor via Grip of Chaos.
 *
 *   Grip of Chaos — {4}{R}{R} Enchantment
 *   Whenever a spell or ability is put onto the stack, if it has a single target,
 *   reselect its target at random.
 *
 * Because reselection is random, single-shot tests can't assert that a redirect
 * actually happened. These tests either:
 *   - run many iterations and assert that at least one redirect occurred, or
 *   - exercise a path where reselection is structurally a no-op (0 or multiple
 *     targets, no other legal targets, etc.).
 */
class GripOfChaosTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun singleTargetOfTopSpell(driver: GameTestDriver): EntityId? {
        val spellId = driver.state.stack.firstOrNull() ?: return null
        val targets = driver.state.getEntity(spellId)
            ?.get<TargetsComponent>()?.targets ?: return null
        if (targets.size != 1) return null
        return when (val t = targets.first()) {
            is ChosenTarget.Permanent -> t.entityId
            is ChosenTarget.Player -> t.playerId
            is ChosenTarget.Card -> t.cardId
            is ChosenTarget.Spell -> t.spellEntityId
        }
    }

    test("single-target spell gets redirected to a different legal target at least once") {
        var redirected = false
        repeat(40) {
            if (redirected) return@repeat
            val driver = createDriver()
            driver.initMirrorMatch(
                deck = Deck.of("Mountain" to 40),
                startingLife = 20
            )
            val activePlayer = driver.activePlayer!!
            val opponent = driver.getOpponent(activePlayer)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(activePlayer, "Grip of Chaos")
            val initial = driver.putCreatureOnBattlefield(opponent, "Glory Seeker")
            driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

            driver.giveMana(activePlayer, Color.RED, 1)
            val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
            driver.castSpellWithTargets(
                activePlayer, bolt, listOf(ChosenTarget.Permanent(initial))
            )

            // Stack: Bolt (bottom), Grip trigger (top). Resolve only the trigger.
            driver.passPriority(driver.state.priorityPlayerId!!)
            driver.passPriority(driver.state.priorityPlayerId!!)

            val newTarget = singleTargetOfTopSpell(driver)
            if (newTarget != null && newTarget != initial) {
                redirected = true
                driver.events.any { it is TargetReselectedEvent } shouldBe true
            }
        }
        redirected shouldBe true
    }

    test("with only one legal target, reselection emits no TargetReselectedEvent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Grip of Chaos")
        val lone = driver.putCreatureOnBattlefield(opponent, "Glory Seeker")

        // Cast Giant Growth (TargetCreature) — only one creature in play on either side
        // is a legal creature target: the opponent's Glory Seeker. The active player
        // controls only Grip of Chaos (an enchantment), so it is not a legal target.
        driver.giveMana(activePlayer, Color.GREEN, 1)
        val growth = driver.putCardInHand(activePlayer, "Giant Growth")
        driver.castSpellWithTargets(
            activePlayer, growth, listOf(ChosenTarget.Permanent(lone))
        )

        // Capture events seen before resolving so we can check after.
        val eventsBefore = driver.events.size

        driver.passPriority(driver.state.priorityPlayerId!!)
        driver.passPriority(driver.state.priorityPlayerId!!)

        // Reselection ran but chose the same (only) target → no reselected event.
        val newEvents = driver.events.drop(eventsBefore)
        newEvents.any { it is TargetReselectedEvent } shouldBe false

        // Target on the stack is still the same creature.
        singleTargetOfTopSpell(driver) shouldBe lone
    }

    test("no-target spell triggers Grip but reselection is a no-op") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Grip of Chaos")

        // Careful Study (instant: "Draw two cards, then discard two cards.") has no targets.
        val library = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        library.isEmpty() shouldBe false
        driver.giveMana(activePlayer, Color.BLACK, 1)
        val study = driver.putCardInHand(activePlayer, "Careful Study")
        val cast = driver.castSpell(activePlayer, study)
        (cast.isSuccess || cast.isPaused) shouldBe true

        // Drain the stack (spell + trigger). There should be no reselection event at all.
        while (driver.state.stack.isNotEmpty()) {
            val before = driver.state.stack.size
            driver.bothPass()
            if (driver.state.stack.size == before && driver.pendingDecision != null) {
                driver.autoResolveDecision()
            }
        }

        driver.events.any { it is TargetReselectedEvent } shouldBe false
    }
})
