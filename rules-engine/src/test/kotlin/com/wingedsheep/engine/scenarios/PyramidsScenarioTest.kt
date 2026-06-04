package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.sba.creature.LethalDamageCheck
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Pyramids (ARN) and the new pieces it introduces:
 *  - `SerializableModification.RemoveDamageShield` — one-shot destruction shield that
 *    swaps "destroyed" for "remove all damage marked on it", without tapping the
 *    permanent or removing it from combat.
 *  - `StatePredicate.AttachedToCardType` — filter for Auras (or Equipment) attached
 *    to a specific top-level type (here, a land).
 *
 * Pyramids itself is exercised by exercising those primitives in the same context the
 * card's modal activated ability would: a land with the shield up survives the next
 * destroy, an Aura attached to a land is recognised by the attached-to-land predicate,
 * and the shield expires at end of turn.
 */
class PyramidsScenarioTest : FunSpec({

    val evaluator = PredicateEvaluator()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Drop a one-shot RemoveDamageShield onto the entity, lasting until end of turn. */
    fun GameTestDriver.addRemoveDamageShield(entityId: EntityId, controllerId: EntityId) {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.RemoveDamageShield,
                affectedEntities = setOf(entityId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = controllerId,
            timestamp = System.currentTimeMillis()
        )
        replaceState(state.copy(floatingEffects = state.floatingEffects + floatingEffect))
    }

    /** Stamp a fixed amount of damage onto an entity (no DamageEvent — direct component write). */
    fun GameTestDriver.markDamage(entityId: EntityId, amount: Int) {
        replaceState(state.updateEntity(entityId) { c -> c.with(DamageComponent(amount)) })
    }

    test("shield replaces the next destroy with damage removal — land survives, damage cleared, untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(active, "Forest")

        // Mark some damage to verify it's stripped by the replacement.
        driver.markDamage(land, 3)
        driver.addRemoveDamageShield(land, active)

        val result = ZoneMovementUtils.destroyPermanent(driver.state, land)
        driver.replaceState(result.state)

        // Land still on the battlefield.
        driver.state.getEntity(land) shouldNotBe null
        // Damage removed by the replacement.
        driver.state.getEntity(land)?.get<DamageComponent>() shouldBe null
        // The shield must NOT tap the permanent — Pyramids only removes damage.
        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe false
        // Shield was consumed.
        driver.state.floatingEffects.none {
            it.effect.modification is SerializableModification.RemoveDamageShield &&
                land in it.effect.affectedEntities
        } shouldBe true
    }

    test("shield is one-shot — second destroy this turn goes through") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(active, "Forest")
        driver.addRemoveDamageShield(land, active)

        // First destroy is replaced — the land stays on the battlefield.
        val first = ZoneMovementUtils.destroyPermanent(driver.state, land)
        driver.replaceState(first.state)
        driver.findPermanent(active, "Forest") shouldNotBe null

        // Second destroy with no shield left — the land really is destroyed.
        val second = ZoneMovementUtils.destroyPermanent(driver.state, land)
        driver.replaceState(second.state)
        driver.findPermanent(active, "Forest") shouldBe null
    }

    test("regeneration shield is preferred — Pyramids' shield stays available for the next destroy") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(active, "Centaur Courser")

        // Both a regen shield and a remove-damage shield target the same creature.
        val regen = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.RegenerationShield,
                affectedEntities = setOf(creature)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = active,
            timestamp = System.currentTimeMillis()
        )
        driver.replaceState(driver.state.copy(floatingEffects = driver.state.floatingEffects + regen))
        driver.addRemoveDamageShield(creature, active)

        val result = ZoneMovementUtils.destroyPermanent(driver.state, creature)
        driver.replaceState(result.state)

        // Creature survived (one of the shields fired).
        driver.state.getEntity(creature) shouldNotBe null
        // Regen ran (it taps as part of its replacement) — confirms regen was preferred.
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe true
        // The Pyramids shield is still around for the next destruction.
        driver.state.floatingEffects.any {
            it.effect.modification is SerializableModification.RemoveDamageShield &&
                creature in it.effect.affectedEntities
        } shouldBe true
    }

    test("shield fires against lethal-damage destruction (SBA) — survives, damage cleared, untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        // A 3/3 stands in for an animated land — the SBA destruction from lethal damage is
        // exactly the "would be destroyed" event Pyramids' second mode replaces.
        val creature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.markDamage(creature, 3)
        driver.addRemoveDamageShield(creature, active)

        val result = LethalDamageCheck().check(driver.state)
        driver.replaceState(result.newState)

        // Survived the lethal-damage SBA because the shield fired.
        driver.state.getEntity(creature) shouldNotBe null
        // Damage was removed (so it's no longer lethal) and the shield does NOT tap.
        driver.state.getEntity(creature)?.get<DamageComponent>() shouldBe null
        driver.state.getEntity(creature)?.has<TappedComponent>() shouldBe false
        // Shield consumed.
        driver.state.floatingEffects.none {
            it.effect.modification is SerializableModification.RemoveDamageShield &&
                creature in it.effect.affectedEntities
        } shouldBe true
    }

    test("AttachedToCardType matches an Aura attached to a land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(active, "Forest")
        // Use a real Aura — Fishliver Oil enchants creatures by oracle text, but for the
        // predicate test we only care that the entity has the Aura subtype and an
        // AttachedToComponent pointing at our land.
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(land)) })

        val predicate = StatePredicate.AttachedToCardType(CardType.LAND)
        val ctx = PredicateContext(controllerId = active)
        evaluator.matchesStatePredicate(driver.state, aura, predicate, ctx) shouldBe true
    }

    test("AttachedToCardType does NOT match an Aura attached to a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val creature = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(creature)) })

        val predicate = StatePredicate.AttachedToCardType(CardType.LAND)
        val ctx = PredicateContext(controllerId = active)
        evaluator.matchesStatePredicate(driver.state, aura, predicate, ctx) shouldBe false
    }

    test("AttachedToCardType is false for an entity with no AttachedToComponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(active, "Forest")

        val predicate = StatePredicate.AttachedToCardType(CardType.LAND)
        val ctx = PredicateContext(controllerId = active)
        evaluator.matchesStatePredicate(driver.state, land, predicate, ctx) shouldBe false
    }

    test("AttachedToCardType reads the attached-to permanent's PROJECTED type — animated land matches CREATURE too") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val active = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(active, "Forest")
        val aura = driver.putPermanentOnBattlefield(active, "Fishliver Oil")
        driver.replaceState(driver.state.updateEntity(aura) { c -> c.with(AttachedToComponent(land)) })

        // Animate the land via a Layer 4 AddType("CREATURE") floating effect — the base
        // CardComponent still says LAND only, but projection should now show CREATURE too.
        val animation = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                modification = SerializableModification.AddType("CREATURE"),
                affectedEntities = setOf(land)
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = active,
            timestamp = System.currentTimeMillis()
        )
        driver.replaceState(driver.state.copy(floatingEffects = driver.state.floatingEffects + animation))

        val ctx = PredicateContext(controllerId = active)
        // LAND remains because projection keeps the base type.
        evaluator.matchesStatePredicate(
            driver.state, aura, StatePredicate.AttachedToCardType(CardType.LAND), ctx
        ) shouldBe true
        // CREATURE is only visible via projection — would fail if the predicate read base CardComponent.
        evaluator.matchesStatePredicate(
            driver.state, aura, StatePredicate.AttachedToCardType(CardType.CREATURE), ctx
        ) shouldBe true
    }
})
