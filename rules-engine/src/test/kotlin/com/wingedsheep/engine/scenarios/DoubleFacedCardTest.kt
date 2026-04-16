package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for double-faced cards (DFCs) — Rule 712.
 *
 * Exercises the transform pipeline:
 *  - [com.wingedsheep.engine.state.components.identity.DoubleFacedComponent] tracks which face is up.
 *  - [com.wingedsheep.engine.handlers.effects.permanent.types.TransformEffectExecutor] swaps
 *    [CardComponent] wholesale and re-registers static abilities for the new face.
 *  - Counters, damage, and other battlefield state persist across a transform.
 */
class DoubleFacedCardTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Island" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("a DFC enters the battlefield on its front face and carries a DoubleFacedComponent") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val entityId = driver.putCreatureOnBattlefield(caster, "Test DFC Front")

        val container = driver.state.getEntity(entityId)
        container.shouldNotBeNull()

        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Test DFC Front"

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.frontCardDefinitionId shouldBe "Test DFC Front"
        dfc.backCardDefinitionId shouldBe "Test DFC Back"
        dfc.currentFace shouldBe DoubleFacedComponent.Face.FRONT
    }

    test("transforming a DFC swaps the CardComponent wholesale to the back face's characteristics") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val entityId = driver.putCreatureOnBattlefield(caster, "Test DFC Front")

        // Cast the transform spell targeting the DFC.
        val spell = driver.putCardInHand(caster, "Transform Target Creature")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell, listOf(entityId)).isSuccess shouldBe true
        driver.bothPass() // resolve the spell

        val container = driver.state.getEntity(entityId)
        container.shouldNotBeNull()

        // CardComponent is now the back face's identity.
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Test DFC Back"
        card.baseStats?.basePower shouldBe 4
        card.baseStats?.baseToughness shouldBe 4

        // DoubleFacedComponent is still present and now shows currentFace=BACK.
        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // Projected P/T matches the back face (4/4).
        val projected = projector.project(driver.state)
        projected.getPower(entityId) shouldBe 4
        projected.getToughness(entityId) shouldBe 4
    }

    test("a DFC can transform back to its front face") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val entityId = driver.putCreatureOnBattlefield(caster, "Test DFC Front")

        // First transform — front → back.
        val spell1 = driver.putCardInHand(caster, "Transform Target Creature")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell1, listOf(entityId)).isSuccess shouldBe true
        driver.bothPass()

        // Second transform — back → front.
        val spell2 = driver.putCardInHand(caster, "Transform Target Creature")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell2, listOf(entityId)).isSuccess shouldBe true
        driver.bothPass()

        val container = driver.state.getEntity(entityId)!!
        val card = container.get<CardComponent>()!!
        val dfc = container.get<DoubleFacedComponent>()!!

        card.name shouldBe "Test DFC Front"
        card.baseStats?.basePower shouldBe 2
        card.baseStats?.baseToughness shouldBe 2
        dfc.currentFace shouldBe DoubleFacedComponent.Face.FRONT
    }

    test("a triggered ability keyed on 'when this transforms' fires through the trigger pipeline") {
        // Back face: "When this transforms, its controller gains 2 life."
        val transformTriggerBack = CardDefinition.creature(
            name = "Transform Trigger Back",
            manaCost = ManaCost.ZERO,
            subtypes = setOf(Subtype("Spirit")),
            power = 3,
            toughness = 3,
            oracleText = "When this transforms, its controller gains 2 life.",
            script = CardScript.permanent(
                triggeredAbilities = listOf(
                    TriggeredAbility(
                        id = AbilityId("transform-trigger"),
                        trigger = Triggers.Transforms.event,
                        binding = Triggers.Transforms.binding,
                        effect = GainLifeEffect(2)
                    )
                )
            )
        )
        val transformTriggerFront = CardDefinition.doubleFacedCreature(
            frontFace = CardDefinition.creature(
                name = "Transform Trigger Front",
                manaCost = ManaCost.parse("{2}{U}"),
                subtypes = setOf(Subtype("Human")),
                power = 1,
                toughness = 1,
                oracleText = ""
            ),
            backFace = transformTriggerBack
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(transformTriggerFront))
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 20),
            skipMulligans = true
        )

        val caster = driver.activePlayer!!
        val startingLife = driver.getLifeTotal(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val entityId = driver.putCreatureOnBattlefield(caster, "Transform Trigger Front")

        val spell = driver.putCardInHand(caster, "Transform Target Creature")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell, listOf(entityId)).isSuccess shouldBe true
        driver.bothPass() // resolve the transform spell

        // The transforms-trigger should now be queued on the stack.
        driver.stackSize shouldBeGreaterThanOrEqual 1
        driver.bothPass() // resolve the trigger

        driver.getLifeTotal(caster) shouldBe (startingLife + 2)
    }

    test("counters persist across a transform") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val entityId = driver.putCreatureOnBattlefield(caster, "Test DFC Front")

        // Put a +1/+1 counter on the permanent.
        driver.replaceState(
            driver.state.updateEntity(entityId) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }
        )

        val spell = driver.putCardInHand(caster, "Transform Target Creature")
        driver.giveMana(caster, Color.BLUE, 2)
        driver.castSpell(caster, spell, listOf(entityId)).isSuccess shouldBe true
        driver.bothPass()

        // Counters survive the transform.
        val counters = driver.state.getEntity(entityId)!!.get<CountersComponent>()
        counters.shouldNotBeNull()
        counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

        // Projected stats are back face (4/4) plus the two +1/+1 counters = 6/6.
        val projected = projector.project(driver.state)
        projected.getPower(entityId) shouldBe 6
        projected.getToughness(entityId) shouldBe 6
    }
})
