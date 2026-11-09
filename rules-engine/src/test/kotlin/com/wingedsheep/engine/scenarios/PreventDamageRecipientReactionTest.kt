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
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
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
})
