package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Deep Freeze
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has base power and toughness 0/4, has defender, loses all other
 * abilities, and is a blue Wall in addition to its other colors and types.
 */
class DeepFreezeTest : FunSpec({

    // A 3/3 creature with flying and an activated ability
    val FlyingCreature = CardDefinition.creature(
        name = "Flying Creature",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Bird")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        script = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = "ping",
                    cost = PayCost.ManaCost("{1}"),
                    description = "{1}: This creature deals 1 damage to any target.",
                    effect = com.wingedsheep.sdk.scripting.effects.DamageEffects.DealDamageEffect(
                        amount = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1),
                        target = com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller
                    )
                )
            )
        )
    )

    test("Deep Freeze sets base P/T to 0/4") {
        val driver = GameTestDriver()
        driver.cardRegistry.register(FlyingCreature)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Deep Freeze" to 10))

        val activePlayer = driver.activePlayer
        val opponent = driver.opponent

        driver.putOnBattlefield(opponent, "Flying Creature")

        driver.giveColorlessMana(activePlayer, 2)
        driver.giveBlueMana(activePlayer, 1)
        val creature = driver.getCreaturesOnBattlefield(opponent).first()
        driver.castSpellTargeting(activePlayer, "Deep Freeze", creature)
        driver.resolveTopOfStack()

        val projected = driver.state.projectedState
        projected.getPower(creature) shouldBe 0
        projected.getToughness(creature) shouldBe 4
    }

    test("Deep Freeze grants defender and removes flying") {
        val driver = GameTestDriver()
        driver.cardRegistry.register(FlyingCreature)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Deep Freeze" to 10))

        val activePlayer = driver.activePlayer
        val opponent = driver.opponent

        driver.putOnBattlefield(opponent, "Flying Creature")

        driver.giveColorlessMana(activePlayer, 2)
        driver.giveBlueMana(activePlayer, 1)
        val creature = driver.getCreaturesOnBattlefield(opponent).first()
        driver.castSpellTargeting(activePlayer, "Deep Freeze", creature)
        driver.resolveTopOfStack()

        val projected = driver.state.projectedState
        projected.hasKeyword(creature, Keyword.DEFENDER) shouldBe true
        projected.hasKeyword(creature, Keyword.FLYING) shouldBe false
    }

    test("Deep Freeze adds Wall subtype and blue color") {
        val driver = GameTestDriver()
        driver.cardRegistry.register(FlyingCreature)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Deep Freeze" to 10))

        val activePlayer = driver.activePlayer
        val opponent = driver.opponent

        driver.putOnBattlefield(opponent, "Flying Creature")

        driver.giveColorlessMana(activePlayer, 2)
        driver.giveBlueMana(activePlayer, 1)
        val creature = driver.getCreaturesOnBattlefield(opponent).first()
        driver.castSpellTargeting(activePlayer, "Deep Freeze", creature)
        driver.resolveTopOfStack()

        val projected = driver.state.projectedState
        projected.hasSubtype(creature, "Wall") shouldBe true
        projected.hasSubtype(creature, "Bird") shouldBe true // keeps original types
        projected.hasColor(creature, Color.BLUE) shouldBe true
    }

    test("Deep Freeze suppresses activated abilities") {
        val driver = GameTestDriver()
        driver.cardRegistry.register(FlyingCreature)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Deep Freeze" to 10))

        val activePlayer = driver.activePlayer
        val opponent = driver.opponent

        driver.putOnBattlefield(opponent, "Flying Creature")

        driver.giveColorlessMana(activePlayer, 2)
        driver.giveBlueMana(activePlayer, 1)
        val creature = driver.getCreaturesOnBattlefield(opponent).first()
        driver.castSpellTargeting(activePlayer, "Deep Freeze", creature)
        driver.resolveTopOfStack()

        val projected = driver.state.projectedState
        projected.hasLostAllAbilities(creature) shouldBe true
    }
})
