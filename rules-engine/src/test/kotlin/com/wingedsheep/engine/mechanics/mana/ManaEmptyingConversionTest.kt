package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The mana-pool half of `ConvertEmptyingMana` ("If you would lose unspent mana, that mana becomes
 * [color] instead" — CR 500.5 emptying, replaced per CR 614.1a) and the `ManaColorSet.ColorsOf`
 * resolver that "any combination of its colors" draws from.
 */
class ManaEmptyingConversionTest : FunSpec({

    context("emptyAtBoundary with a conversion colour") {

        test("plain coloured and colorless mana all become that many of the colour") {
            val pool = ManaPoolComponent(white = 1, red = 2, colorless = 1)
            val after = pool.emptyAtBoundary(convertTo = Color.BLACK, retain = emptySet())
            after.black shouldBe 4
            after.total shouldBe 4
        }

        test("a restricted unit is recoloured but keeps its restriction and riders") {
            val pool = ManaPoolComponent()
                .addRestricted(Color.GREEN, 1, ManaRestriction.InstantOrSorceryOnly)
                .addRestricted(Color.RED, 1, ManaRestriction.AnySpend, riders = setOf(ManaSpellRider.CopySpellWhenSpent(com.wingedsheep.sdk.scripting.GameObjectFilter.InstantOrSorcery)))
            val after = pool.emptyAtBoundary(convertTo = Color.BLACK, retain = emptySet())
            after.black shouldBe 0
            after.restrictedMana.map { Triple(it.color, it.restriction, it.riders) } shouldBe listOf(
                Triple(Color.BLACK, ManaRestriction.InstantOrSorceryOnly, emptySet()),
                Triple(Color.BLACK, ManaRestriction.AnySpend, setOf(ManaSpellRider.CopySpellWhenSpent(com.wingedsheep.sdk.scripting.GameObjectFilter.InstantOrSorcery))),
            )
        }

        test("conversion wins over retention") {
            val pool = ManaPoolComponent(red = 1, blue = 1)
            val after = pool.emptyAtBoundary(convertTo = Color.BLACK, retain = setOf(Color.RED))
            after.black shouldBe 2
            after.red shouldBe 0
        }

        test("combat-duration mana is left for end of combat") {
            val pool = ManaPoolComponent(blue = 1)
                .addRestricted(Color.RED, 2, ManaRestriction.AnySpend, expiry = ManaExpiry.END_OF_COMBAT)
            val after = pool.emptyAtBoundary(convertTo = Color.BLACK, retain = emptySet())
            after.black shouldBe 1
            after.restrictedMana.size shouldBe 2
            after.restrictedMana.all { it.color == Color.RED && it.expiry == ManaExpiry.END_OF_COMBAT } shouldBe true
        }

        test("without a conversion the pool empties") {
            ManaPoolComponent(red = 3).emptyAtBoundary(convertTo = null, retain = emptySet()).total shouldBe 0
        }
    }

    context("convertExpired at end of combat") {

        test("firebending mana becomes plain mana of the colour; a restricted unit stays restricted") {
            val pool = ManaPoolComponent(green = 1)
                .addRestricted(Color.RED, 2, ManaRestriction.AnySpend, expiry = ManaExpiry.END_OF_COMBAT)
                .addRestricted(Color.RED, 1, ManaRestriction.CreatureSpellsOnly, expiry = ManaExpiry.END_OF_COMBAT)
            val after = pool.convertExpired(ManaExpiry.END_OF_COMBAT, Color.BLACK)
            after.black shouldBe 2
            after.green shouldBe 1
            after.restrictedMana.size shouldBe 1
            after.restrictedMana.single().color shouldBe Color.BLACK
            after.restrictedMana.single().restriction shouldBe ManaRestriction.CreatureSpellsOnly
            after.restrictedMana.single().expiry shouldBe ManaExpiry.END_OF_TURN
        }
    }

    context("ManaColorSet.ColorsOf") {

        test("reads the named object's colours in any zone, nothing when it can't be resolved") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val bearsInLibrary = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
            val colorsOf = ManaColorSet.ColorsOf(EffectTarget.PipelineTarget("looked"))

            fun resolve(resolved: com.wingedsheep.sdk.model.EntityId?, sourceId: com.wingedsheep.sdk.model.EntityId? = null) =
                ManaColorSetResolver.resolve(
                    colorSet = colorsOf,
                    state = driver.state,
                    projected = driver.state.projectedState,
                    sourceId = sourceId,
                    controllerId = player,
                    cardRegistry = driver.cardRegistry,
                    predicateEvaluator = driver.services.predicateEvaluator,
                    resolveEntity = { resolved }
                )

            resolve(bearsInLibrary) shouldBe setOf(Color.GREEN)
            resolve(null) shouldBe emptySet()
        }

        test("outside an effect only Self resolves — to the source") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

            fun resolve(target: EffectTarget) = ManaColorSetResolver.resolve(
                colorSet = ManaColorSet.ColorsOf(target),
                state = driver.state,
                projected = driver.state.projectedState,
                sourceId = bears,
                controllerId = player,
                cardRegistry = driver.cardRegistry,
                predicateEvaluator = driver.services.predicateEvaluator
            )

            resolve(EffectTarget.Self) shouldBe setOf(Color.GREEN)
            resolve(EffectTarget.PipelineTarget("looked")) shouldBe emptySet()
        }
    }
})
