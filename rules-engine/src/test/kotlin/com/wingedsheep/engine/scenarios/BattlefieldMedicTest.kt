package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AbilityFizzledEvent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.PreventNextDamageEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature
import com.wingedsheep.sdk.targeting.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Battlefield Medic and the PreventNextDamage mechanic.
 *
 * Battlefield Medic: {1}{W}
 * Creature — Human Cleric
 * 1/1
 * {T}: Prevent the next X damage that would be dealt to target creature this turn,
 * where X is the number of Clerics on the battlefield.
 */
class BattlefieldMedicTest : FunSpec({

    val medicAbilityId = AbilityId(UUID.randomUUID().toString())

    val BattlefieldMedic = CardDefinition.creature(
        name = "Battlefield Medic",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 1,
        toughness = 1,
        oracleText = "{T}: Prevent the next X damage that would be dealt to target creature this turn, where X is the number of Clerics on the battlefield.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = medicAbilityId,
                cost = AbilityCost.Tap,
                effect = PreventNextDamageEffect(
                    amount = DynamicAmounts.creaturesWithSubtype(Subtype("Cleric")),
                    target = EffectTarget.ContextTarget(0)
                ),
                targetRequirement = TargetPermanent(filter = TargetFilter.Creature)
            )
        )
    )

    // A second Cleric to increase the count
    val TestCleric = CardDefinition.creature(
        name = "Test Cleric",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    // A big creature to receive the shield
    val HillGiant = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 3,
        toughness = 3,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BattlefieldMedic, TestCleric, HillGiant))
        return driver
    }

    test("basic prevention - shield prevents all damage when amount equals cleric count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put medic and a target creature on the battlefield
        val medic = driver.putCreatureOnBattlefield(activePlayer, "Battlefield Medic")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(medic)

        // Activate medic's ability targeting the Hill Giant
        // Only 1 Cleric on battlefield (the medic itself), so X = 1
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        activateResult.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Now deal 1 damage to the Hill Giant via Lightning Bolt (3 damage)
        // X = 1 (one Cleric), so 1 damage prevented, 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        // Lightning Bolt deals 3 damage, shield prevents 1, so 2 damage gets through
        val damage = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 2
    }

    test("partial prevention - shield prevents some damage, rest gets through") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put medic + extra cleric + target creature
        val medic = driver.putCreatureOnBattlefield(activePlayer, "Battlefield Medic")
        driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(medic)

        // Activate medic's ability targeting the Hill Giant
        // 2 Clerics on battlefield (medic + Test Cleric), so X = 2
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        // Deal 3 damage via Lightning Bolt - shield prevents 2, 1 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        val damage = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 1
    }

    test("prevention scales with cleric count - three clerics prevent 3 damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put medic + 2 extra clerics + target creature
        val medic = driver.putCreatureOnBattlefield(activePlayer, "Battlefield Medic")
        driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(medic)

        // 3 Clerics on battlefield, so X = 3
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        // Deal 3 damage via Lightning Bolt - shield prevents all 3, 0 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        val damage = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 0
    }

    test("shield prevents combat damage to creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put medic + extra cleric + our attacker + opponent's blocker
        val medic = driver.putCreatureOnBattlefield(activePlayer, "Battlefield Medic")
        driver.putCreatureOnBattlefield(activePlayer, "Test Cleric") // 2 clerics total
        val ourCreature = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(medic)
        driver.removeSummoningSickness(ourCreature)

        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Hill Giant")
        driver.removeSummoningSickness(opponentCreature)

        // Activate medic's ability targeting our Hill Giant (shield = 2 from 2 Clerics)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(ourCreature))
            )
        )
        driver.bothPass()

        // Move to combat on our turn - we attack with Hill Giant
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(ourCreature), opponent)
        driver.bothPass()

        // Opponent blocks with their Hill Giant
        driver.declareBlockers(opponent, mapOf(opponentCreature to listOf(ourCreature)))
        driver.bothPass()

        // First strike damage step (no first strikers)
        driver.bothPass()

        // Combat damage step - opponent's 3/3 deals 3 to our creature, shield prevents 2, 1 gets through
        driver.bothPass()

        val ourCreatureDamage = driver.state.getEntity(ourCreature)?.get<DamageComponent>()?.amount ?: 0
        ourCreatureDamage shouldBe 1
    }

    test("shield is consumed after preventing damage - subsequent damage is not prevented") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put medic and target creature (1 Cleric = shield of 1)
        val medic = driver.putCreatureOnBattlefield(activePlayer, "Battlefield Medic")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(medic)

        // Activate medic's ability targeting the Hill Giant (shield = 1 from 1 Cleric)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        driver.bothPass()

        // Verify shield is active
        driver.state.floatingEffects.any {
            it.effect.modification is com.wingedsheep.engine.mechanics.layers.SerializableModification.PreventNextDamage
        } shouldBe true

        // First bolt: 3 damage, shield prevents 1, 2 gets through. Shield consumed.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt1, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        val damage1 = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage1 shouldBe 2

        // Shield should be consumed now
        driver.state.floatingEffects.any {
            it.effect.modification is com.wingedsheep.engine.mechanics.layers.SerializableModification.PreventNextDamage
        } shouldBe false

        // Second bolt: full 3 damage, no shield. Total = 2 + 3 = 5 >= 3 toughness = lethal.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt2, listOf(ChosenTarget.Permanent(target))).isSuccess shouldBe true
        driver.bothPass()

        // Creature took lethal damage (5 >= 3 toughness) — destroyed by state-based actions.
        // DamageComponent is stripped when moved to graveyard.
        val damage2 = driver.state.getEntity(target)?.get<DamageComponent>()?.amount
        // Either creature has 5 damage or was destroyed (null). Key test: shield was consumed above.
        if (damage2 != null) {
            damage2 shouldBe 5
        }
    }

    test("ability fizzles when target gains shroud while ability is on the stack (Rule 608.2b)") {
        val MagesGuile = CardDefinition.instant(
            name = "Mage's Guile",
            manaCost = ManaCost.parse("{1}{U}"),
            oracleText = "Target creature gains shroud until end of turn.",
            script = CardScript.spell(
                effect = GrantKeywordUntilEndOfTurnEffect(Keyword.SHROUD, EffectTarget.ContextTarget(0)),
                TargetCreature()
            )
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BattlefieldMedic, HillGiant, MagesGuile))
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Island" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1: Medic + target creature on battlefield
        val medic = driver.putCreatureOnBattlefield(p1, "Battlefield Medic")
        val target = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(medic)

        // P1 activates Medic ability targeting Hill Giant → goes on stack
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = medic,
                abilityId = medicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        activateResult.isSuccess shouldBe true

        // P1 passes priority
        driver.passPriority(p1)

        // P2 casts Mage's Guile targeting Hill Giant in response → goes on stack above
        val guile = driver.putCardInHand(p2, "Mage's Guile")
        driver.giveMana(p2, Color.BLUE, 2)
        val castResult = driver.castSpellWithTargets(p2, guile, listOf(ChosenTarget.Permanent(target)))
        castResult.isSuccess shouldBe true

        // Both pass → Mage's Guile resolves (LIFO) → Hill Giant gains shroud
        driver.bothPass()

        // Both pass → Medic ability resolves → target has shroud → fizzle!
        driver.bothPass()

        // The ability should have fizzled
        val fizzled = driver.events.any { it is AbilityFizzledEvent }
        fizzled shouldBe true

        // No damage prevention shield should exist (ability fizzled, never created the shield)
        driver.state.floatingEffects.none {
            it.effect.modification is com.wingedsheep.engine.mechanics.layers.SerializableModification.PreventNextDamage
        } shouldBe true
    }
})
