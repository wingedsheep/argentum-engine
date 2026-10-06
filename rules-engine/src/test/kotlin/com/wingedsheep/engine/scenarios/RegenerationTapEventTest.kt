package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.isFirstTapThisTurn
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.HasBecomeTappedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Regeneration's tap is a **real tap transition**, not a silent state write.
 *
 * CR 701.19a defines "Regenerate [permanent]" as "…instead remove all damage marked on it and *its
 * controller taps it*. If it's an attacking or blocking creature, remove it from combat." That tap
 * therefore has to behave like every other tap: emit a [TappedEvent] so "becomes tapped" triggers
 * see it (Deeproot Pilgrimage; Captain America, Living Legend), and stamp the per-permanent
 * first-time-tapped window so a *later* tap that turn is correctly not the creature's first.
 *
 * These are the two directions that were wrong while `applyRegenerationReplacement` open-coded
 * `with(TappedComponent)`: the trigger never fired, and the unstamped window handed out a second
 * "first tap" later in the same turn.
 *
 * The already-tapped case pins CR 701.26a ("only untapped permanents can be tapped"): there is no
 * transition, so no event — but the rest of the replacement still applies.
 *
 * Combat interactions of regeneration (removal from combat, damage timing) live in
 * [RegenerationCombatTest]; this file is only about the tap.
 */
class RegenerationTapEventTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun GameTestDriver.addRegenerationShield(entityId: EntityId, controllerId: EntityId) {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.RegenerationShield,
                affectedEntities = setOf(entityId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = controllerId,
            timestamp = System.currentTimeMillis()
        )
        replaceState(state.copy(floatingEffects = state.floatingEffects + floatingEffect))
    }

    test("regenerating an untapped creature emits a TappedEvent attributed to its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.addRegenerationShield(creature, player)

        val (shieldState, wasRegenerated) =
            ZoneMovementUtils.applyRegenerationShields(driver.state, creature)
        wasRegenerated shouldBe true

        val result = ZoneMovementUtils.applyRegenerationReplacement(shieldState, creature)

        val tapped = result.events.filterIsInstance<TappedEvent>()
        withClue("regeneration taps, so exactly one TappedEvent is emitted") {
            tapped.size shouldBe 1
        }
        withClue("CR 701.19a: *its controller* taps it") {
            tapped.single().entityId shouldBe creature
            tapped.single().tappedById shouldBe player
        }
        withClue("this was the creature's first tap this turn") {
            tapped.single().firstThisTurn shouldBe true
        }
        result.state.getEntity(creature)?.has<TappedComponent>() shouldBe true
    }

    test("regenerating an untapped creature stamps the first-time-tapped window") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.addRegenerationShield(creature, player)

        withClue("nothing has tapped it yet this turn") {
            isFirstTapThisTurn(driver.state, creature) shouldBe true
        }

        val (shieldState, _) = ZoneMovementUtils.applyRegenerationShields(driver.state, creature)
        val result = ZoneMovementUtils.applyRegenerationReplacement(shieldState, creature)

        val stamp = result.state.getEntity(creature)?.get<HasBecomeTappedComponent>().shouldNotBeNull()
        stamp.lastBecameTappedTurn shouldBe result.state.turnNumber

        withClue("the window is spent — a later tap this turn is not the creature's first") {
            isFirstTapThisTurn(result.state, creature) shouldBe false
        }
    }

    test("regenerating an already tapped creature emits no event but still removes damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.tapPermanent(creature)
        driver.replaceState(
            driver.state.updateEntity(creature) { it.with(DamageComponent(amount = 3)) }
        )
        driver.addRegenerationShield(creature, player)

        val (shieldState, _) = ZoneMovementUtils.applyRegenerationShields(driver.state, creature)
        val result = ZoneMovementUtils.applyRegenerationReplacement(shieldState, creature)

        withClue("CR 701.26a: only untapped permanents can be tapped, so this is no transition") {
            result.events.filterIsInstance<TappedEvent>().shouldBeEmpty()
        }
        withClue("it stays tapped, and the rest of the replacement still applies") {
            result.state.getEntity(creature)?.has<TappedComponent>() shouldBe true
            result.state.getEntity(creature)?.get<DamageComponent>() shouldBe null
        }
    }

    test("a creature that attacked and then regenerated does not get a second first-tap that turn") {
        // The stale-window direction: the attack tap stamps, regeneration must not reopen it.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.removeSummoningSickness(creature)
        driver.tapPermanent(creature)
        driver.untapPermanent(creature)

        // Stamp the window the way a real tap would, then regenerate while untapped.
        val (tappedState, firstEvent) = com.wingedsheep.engine.core.tap(driver.state, creature).let { it.state to it.tapped }
        firstEvent.shouldNotBeNull().firstThisTurn shouldBe true
        driver.replaceState(tappedState)
        driver.untapPermanent(creature)
        driver.addRegenerationShield(creature, player)

        val (shieldState, _) = ZoneMovementUtils.applyRegenerationShields(driver.state, creature)
        val result = ZoneMovementUtils.applyRegenerationReplacement(shieldState, creature)

        withClue("the window was already spent, so regeneration's tap is not a first tap") {
            result.events.filterIsInstance<TappedEvent>().single().firstThisTurn shouldBe false
        }
    }
})
