package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The plain targeted source shield — `PreventDamage(target = creature, direction = FromTarget,
 * nextInstanceOnly = true)` with no `onPrevented` reaction: "the next time target creature would
 * deal damage this turn, prevent that damage" (Dazzling Reflection). It prevents only the
 * creature's next damage event, whatever it hits, and is then spent; two such shields each take
 * one later event.
 */
class PreventNextDamageFromTargetScenarioTest : FunSpec({

    val ward = card("Test Next-Time Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = creature,
                direction = PreventionDirection.FromTarget,
                nextInstanceOnly = true,
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
            effect = Effects.DealDamage(1, any)
        }
    }

    // "{0}: This creature deals 1 damage to each creature." — one damage event, several recipients.
    val sweeper = card("Test Sweeper") {
        manaCost = "{1}"
        typeLine = "Creature — Shaman"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature), Effects.DealDamage(1, EffectTarget.IterationEntity))
        }
    }

    val wall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    fun shields(driver: GameTestDriver): List<SerializableModification.PreventNextDamageFromSourceShield> =
        driver.state.floatingEffects.mapNotNull {
            it.effect.modification as? SerializableModification.PreventNextDamageFromSourceShield
        }

    data class Setup(val driver: GameTestDriver, val caster: EntityId, val opponent: EntityId, val pingerId: EntityId)

    fun setUp(wards: Int): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ward, pinger, sweeper, wall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val pingerId = driver.putCreatureOnBattlefield(opponent, "Test Pinger")
        driver.removeSummoningSickness(pingerId)
        repeat(wards) {
            val wardCard = driver.putCardInHand(caster, "Test Next-Time Ward")
            driver.putLandOnBattlefield(caster, "Plains")
            driver.castSpellWithTargets(caster, wardCard, listOf(ChosenTarget.Permanent(pingerId))).error shouldBe null
            driver.bothPass()
        }
        return Setup(driver, caster, opponent, pingerId)
    }

    fun Setup.ping(target: ChosenTarget) {
        val ping = driver.cardRegistry.requireCard("Test Pinger").activatedAbilities.single()
        driver.passPriority(caster)
        driver.submit(ActivateAbility(playerId = opponent, sourceId = pingerId, abilityId = ping.id, targets = listOf(target)))
            .error shouldBe null
        driver.bothPass()
    }

    test("prevents only the creature's next damage, to any recipient, with no linked reaction") {
        val setup = setUp(wards = 1)
        val shield = shields(setup.driver).single()
        shield.damageSourceId shouldBe setup.pingerId
        shield.hasReaction shouldBe false
        setup.driver.state.delayedTriggers.size shouldBe 0
        // The shield is keyed to the damage source, so the source carries the badge.
        val view = ClientStateTransformer(setup.driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = setup.driver.cardRegistry))
        val badge = view.transform(setup.driver.state, setup.opponent).cards.getValue(setup.pingerId).activeEffects
            .single { it.effectId.startsWith("source_shield_") }
        badge.name shouldBe "Prevent next damage"
        badge.description shouldBe "The next time this would deal damage, that damage is prevented (until end of turn)"
        val wallId = setup.driver.putCreatureOnBattlefield(setup.caster, "Test Wall")

        setup.ping(ChosenTarget.Permanent(wallId))
        (setup.driver.state.getEntity(wallId)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        shields(setup.driver).size shouldBe 0

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 19)
    }

    test("two shields on the same creature each prevent one later damage event") {
        val setup = setUp(wards = 2)
        shields(setup.driver).size shouldBe 2

        // Both shields apply to the first ping: the damaged player picks which one is spent (CR 616.1).
        setup.ping(ChosenTarget.Player(setup.caster))
        val order = setup.driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        order.options.size shouldBe 2
        setup.driver.submitDecision(setup.caster, OptionChosenResponse(order.id, 0)).error shouldBe null
        setup.driver.bothPass()
        setup.driver.assertLifeTotal(setup.caster, 20)
        shields(setup.driver).size shouldBe 1

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 20)
        shields(setup.driver).size shouldBe 0
        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 19)
    }

    test("one damage event to several recipients is prevented in full and spends the shield once") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ward, pinger, sweeper, wall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val sweeperId = driver.putCreatureOnBattlefield(opponent, "Test Sweeper")
        driver.removeSummoningSickness(sweeperId)
        val walls = listOf(driver.putCreatureOnBattlefield(caster, "Test Wall"), driver.putCreatureOnBattlefield(caster, "Test Wall"))
        val wardCard = driver.putCardInHand(caster, "Test Next-Time Ward")
        driver.putLandOnBattlefield(caster, "Plains")
        driver.castSpellWithTargets(caster, wardCard, listOf(ChosenTarget.Permanent(sweeperId))).error shouldBe null
        driver.bothPass()

        val sweep = driver.cardRegistry.requireCard("Test Sweeper").activatedAbilities.single()
        fun sweepOnce() {
            driver.passPriority(caster)
            driver.submit(ActivateAbility(playerId = opponent, sourceId = sweeperId, abilityId = sweep.id)).error shouldBe null
            driver.bothPass()
        }
        fun damageOn(id: EntityId) = driver.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        sweepOnce()
        walls.map(::damageOn) shouldBe listOf(0, 0)
        damageOn(sweeperId) shouldBe 0
        shields(driver).size shouldBe 0

        sweepOnce()
        walls.map(::damageOn) shouldBe listOf(1, 1)
    }
})
