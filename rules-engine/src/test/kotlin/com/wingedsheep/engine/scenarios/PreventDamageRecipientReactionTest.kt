package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.PreventDamageExecutor
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The reaction on a recipient-side shield — `PreventDamage(alsoToYou / toGroup, onPrevented = …)`
 * ("for each 1 damage prevented this way, …", Inkshield).
 *
 * Every application of the shield (each damage instance it prevents) queues the reaction with that
 * instance's prevented amount; damage the shield doesn't cover queues nothing, and a shield shape
 * with no lowering for a reaction fails rather than drop it.
 */
class PreventDamageRecipientReactionTest : FunSpec({

    // "Prevent all damage that would be dealt to you and creatures you control this turn. For each
    // 1 damage prevented this way, you gain 1 life."
    val groupWard = card("Test Group Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            effect = Effects.PreventDamage(
                toGroup = GameObjectFilter.Creature.youControl(),
                alsoToYou = true,
                onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()),
            )
        }
    }

    val pinger = card("Test Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Wizard"
        power = 1
        toughness = 1
        activatedAbility {
            val any = target(Targets.Any)
            cost = Costs.Mana("{0}")
            effect = Effects.DealDamage(2, any)
        }
    }

    val wall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    class Setup(val driver: GameTestDriver, val caster: com.wingedsheep.sdk.model.EntityId,
                val opponent: com.wingedsheep.sdk.model.EntityId, val pingerId: com.wingedsheep.sdk.model.EntityId)

    fun setUp(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(groupWard, pinger, wall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val ward = driver.putCardInHand(caster, "Test Group Ward")
        driver.putLandOnBattlefield(caster, "Plains")
        val pingerId = driver.putCreatureOnBattlefield(opponent, "Test Pinger")
        driver.removeSummoningSickness(pingerId)
        driver.castSpell(caster, ward, emptyList()).error shouldBe null
        driver.bothPass()
        driver.state.floatingEffects.count {
            (it.effect.modification as? SerializableModification.PreventAllDamageToGroup)?.onPrevented != null
        } shouldBe 1
        return Setup(driver, caster, opponent, pingerId)
    }

    fun Setup.ping(target: ChosenTarget) {
        val ability = driver.cardRegistry.requireCard("Test Pinger").activatedAbilities.single()
        driver.passPriority(caster)
        driver.submit(ActivateAbility(playerId = opponent, sourceId = pingerId, abilityId = ability.id, targets = listOf(target)))
            .error shouldBe null
        driver.bothPass()
    }

    test("each application runs the reaction with what it prevented — a recipient creature and the player both pay out") {
        val setup = setUp()
        val wallId = setup.driver.putCreatureOnBattlefield(setup.caster, "Test Wall")

        setup.ping(ChosenTarget.Permanent(wallId))
        setup.driver.state.getEntity(wallId)?.get<DamageComponent>()?.amount shouldBe null
        setup.driver.assertLifeTotal(setup.caster, 22)

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 24)
    }

    test("damage to a recipient the shield doesn't cover is dealt and runs no reaction") {
        val setup = setUp()

        setup.ping(ChosenTarget.Player(setup.opponent))
        setup.driver.assertLifeTotal(setup.opponent, 18)
        setup.driver.assertLifeTotal(setup.caster, 20)
    }

    // "Prevent all damage that would be dealt to you this turn. If damage is prevented this way,
    // ~ deals that much damage to target creature." — the reaction names a target chosen at cast.
    val targetedWard = card("Test Targeted Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                alsoToYou = true,
                onPrevented = Effects.DealDamage(DynamicAmounts.preventedDamage(), creature),
            )
        }
    }

    fun targetedSetUp(): Pair<Setup, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(targetedWard, pinger, wall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val wallId = driver.putCreatureOnBattlefield(opponent, "Test Wall")
        val ward = driver.putCardInHand(caster, "Test Targeted Ward")
        driver.putLandOnBattlefield(caster, "Plains")
        val pingerId = driver.putCreatureOnBattlefield(opponent, "Test Pinger")
        driver.removeSummoningSickness(pingerId)
        driver.castSpell(caster, ward, listOf(wallId)).error shouldBe null
        driver.bothPass()
        return Setup(driver, caster, opponent, pingerId) to wallId
    }

    test("the reaction names the target its spell chose as it was cast") {
        val (setup, wallId) = targetedSetUp()
        val shield = setup.driver.state.floatingEffects.single {
            it.effect.modification is SerializableModification.PreventAllDamageToGroup
        }.effect.modification as SerializableModification.PreventAllDamageToGroup
        shield.onPreventedTargets shouldBe listOf(ChosenTarget.Permanent(wallId))

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 20)
        setup.driver.state.getEntity(wallId)?.get<DamageComponent>()?.amount shouldBe 2
    }

    test("a target that has become a new object since the shield was made takes nothing (CR 400.7)") {
        val (setup, wallId) = targetedSetUp()
        // The wall left and came back under the same id: its identity generation moved on.
        val driver = setup.driver
        driver.replaceState(driver.state.copy(floatingEffects = driver.state.floatingEffects.map { f ->
            val mod = f.effect.modification as? SerializableModification.PreventAllDamageToGroup ?: return@map f
            f.copy(effect = f.effect.copy(modification = mod.copy(onPreventedBound = mod.onPreventedBound.map {
                it.copy(objectRef = it.objectRef!!.copy(generation = it.objectRef.generation + 1))
            })))
        }))

        setup.ping(ChosenTarget.Player(setup.caster))
        withClue("the damage is still prevented") { driver.assertLifeTotal(setup.caster, 20) }
        driver.state.getEntity(wallId)?.get<DamageComponent>()?.amount shouldBe null
    }

    test("a shield shape that can't carry the reaction fails instead of dropping it") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val executor = PreventDamageExecutor(driver.services.predicateEvaluator.amounts)

        // "Prevent the next 2 damage that would be dealt to you" has no recipient-side lowering for a reaction.
        val unsupported: PreventDamageEffect = PreventDamageEffect(
            amount = DynamicAmounts.fixed(2),
            direction = PreventionDirection.ToTarget,
            onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()),
        )
        val result = executor.execute(driver.state, unsupported, EffectContext(sourceId = null, controllerId = player))
        (result.error != null) shouldBe true
        result.state.floatingEffects.size shouldBe driver.state.floatingEffects.size
    }

    test("a reacting recipient shield rejects the shapes it would silently ignore") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val executor = PreventDamageExecutor(driver.services.predicateEvaluator.amounts)
        val reaction = Effects.GainLife(DynamicAmounts.preventedDamage())

        listOf(
            // "Instead it still deals that damage" (Eye for an Eye's shape) on a "to you" shield.
            PreventDamageEffect(recipientGroupIncludesController = true, preventDamage = false, onPrevented = reaction),
            // A capped amount, which the group shield would widen to "prevent all".
            PreventDamageEffect(recipientGroupIncludesController = true, amount = DynamicAmounts.fixed(2), onPrevented = reaction),
            PreventDamageEffect(recipientGroupIncludesController = true, nextInstanceOnly = true, onPrevented = reaction),
        ).forEach { shape ->
            val result = executor.execute(driver.state, shape, EffectContext(sourceId = null, controllerId = player))
            (result.error != null) shouldBe true
            result.state.floatingEffects.size shouldBe driver.state.floatingEffects.size
        }
    }
})
