package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The recipient and scope filters of the prevent-and-react source shield —
 * [com.wingedsheep.sdk.scripting.effects.PreventDamageEffect.toPlayersOnly] and `combatOnly` on
 * `PreventDamage(direction = FromTarget, onPrevented = …)`.
 *
 * Damage outside the shield's scope — to a permanent when it covers players only, or noncombat when
 * it covers combat only — is dealt in full and leaves the shield up for a later instance that does
 * match. The combat half on a real card is Ria Ivor, Bane of Bladehold's scenario test.
 */
class PreventDamageToPlayersOnlyScenarioTest : FunSpec({

    // "The next time target creature would deal damage to one or more players this turn, prevent
    // that damage. You gain life equal to the damage prevented this way."
    val playerWard = card("Test Player Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = creature,
                direction = PreventionDirection.FromTarget,
                toPlayersOnly = true,
                nextInstanceOnly = true,
                onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()),
            )
        }
    }

    // The same, narrowed to combat damage.
    val combatPlayerWard = card("Test Combat Player Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PreventDamage(
                target = creature,
                direction = PreventionDirection.FromTarget,
                combatOnly = true,
                toPlayersOnly = true,
                nextInstanceOnly = true,
                onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage()),
            )
        }
    }

    // "{0}: This creature deals 1 damage to any target." — an untapped, repeatable noncombat source.
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

    val wall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    fun shields(driver: GameTestDriver): Int = driver.state.floatingEffects.count {
        it.effect.modification is SerializableModification.PreventNextDamageFromSourceShield
    }

    data class Setup(val driver: GameTestDriver, val caster: EntityId, val opponent: EntityId, val pingerId: EntityId)

    fun setUp(ward: String): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(playerWard, combatPlayerWard, pinger, wall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val wardCard = driver.putCardInHand(caster, ward)
        driver.putLandOnBattlefield(caster, "Plains")
        val pingerId = driver.putCreatureOnBattlefield(opponent, "Test Pinger")
        driver.removeSummoningSickness(pingerId)
        driver.castSpellWithTargets(caster, wardCard, listOf(ChosenTarget.Permanent(pingerId))).error shouldBe null
        driver.bothPass()
        shields(driver) shouldBe 1
        driver.passPriority(caster)
        return Setup(driver, caster, opponent, pingerId)
    }

    fun Setup.ping(target: ChosenTarget) {
        val ping = driver.cardRegistry.requireCard("Test Pinger").activatedAbilities.single()
        driver.submit(ActivateAbility(playerId = opponent, sourceId = pingerId, abilityId = ping.id, targets = listOf(target)))
            .error shouldBe null
        driver.bothPass()
    }

    test("players-only: damage to a creature is dealt and keeps the shield; damage to a player is prevented") {
        val setup = setUp("Test Player Ward")
        val bear = setup.driver.putCreatureOnBattlefield(setup.caster, "Test Wall")

        setup.ping(ChosenTarget.Permanent(bear))
        setup.driver.state.getEntity(bear)?.get<DamageComponent>()?.amount shouldBe 1
        shields(setup.driver) shouldBe 1

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.bothPass() // resolve the linked life-gain trigger
        setup.driver.assertLifeTotal(setup.caster, 21)
        shields(setup.driver) shouldBe 0
    }

    test("combat-only: noncombat damage to a player is dealt and keeps the shield") {
        val setup = setUp("Test Combat Player Ward")

        setup.ping(ChosenTarget.Player(setup.caster))
        setup.driver.assertLifeTotal(setup.caster, 19)
        shields(setup.driver) shouldBe 1
    }
})
