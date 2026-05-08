package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Incubate keyword action (CR 701.53).
 *
 * Incubate N creates a transforming double-faced Incubator token with N +1/+1 counters
 * and "{2}: Transform this token." Transforming flips it into a 0/0 colorless Phyrexian
 * artifact creature, with the +1/+1 counters persisting.
 *
 * Implementation strategy: pure composition via [Effects.Incubate], which is a
 * [com.wingedsheep.sdk.scripting.effects.CompositeEffect] of two atomics — create the
 * predefined Incubator token (publishing its entity ID into the pipeline collection
 * [com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS]), then put N +1/+1 counters
 * on the just-created token via `EffectTarget.PipelineTarget`. No Incubate-specific
 * executor exists.
 */
class IncubateTest : FunSpec({

    val projector = StateProjector()

    // Test-only host card that triggers Incubate 3 with no other side effects.
    // Sorcery, free to cast — the test is about the Incubate effect, not casting costs.
    val testIncubateThree = CardDefinition.sorcery(
        name = "Test Incubate 3",
        manaCost = ManaCost.ZERO,
        oracleText = "Incubate 3.",
        script = CardScript.spell(effect = Effects.Incubate(3))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(testIncubateThree))
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("Incubate 3 creates an Incubator token entering as a DFC with 3 +1/+1 counters") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(caster, "Test Incubate 3")
        driver.castSpell(caster, spell, emptyList()).isSuccess shouldBe true
        driver.bothPass() // resolve the Incubate spell

        val tokenId = driver.findPermanent(caster, "Incubator")
        tokenId.shouldNotBeNull()

        val container = driver.state.getEntity(tokenId)!!

        val card = container.get<CardComponent>()!!
        card.name shouldBe "Incubator"

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.frontCardDefinitionId shouldBe "Incubator"
        dfc.backCardDefinitionId shouldBe "Phyrexian"
        dfc.currentFace shouldBe DoubleFacedComponent.Face.FRONT

        val counters = container.get<CountersComponent>()
        counters.shouldNotBeNull()
        counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
    }

    test("activating {2}: Transform flips the Incubator into a Phyrexian; counters persist as a 3/3") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = driver.putCardInHand(caster, "Test Incubate 3")
        driver.castSpell(caster, spell, emptyList()).isSuccess shouldBe true
        driver.bothPass()

        val tokenId = driver.findPermanent(caster, "Incubator")!!

        // The Incubator's `{2}: Transform` ability is declared on its CardDefinition
        // in PredefinedTokens.kt; the DSL auto-generates an AbilityId.
        val incubatorDef = driver.cardRegistry.requireCard("Incubator")
        val transformAbilityId = incubatorDef.script.activatedAbilities.first().id

        driver.giveColorlessMana(caster, 2)
        driver.submit(
            ActivateAbility(playerId = caster, sourceId = tokenId, abilityId = transformAbilityId)
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the activation

        val container = driver.state.getEntity(tokenId)!!

        // Wholesale CardComponent swap: now identifies as Phyrexian.
        val card = container.get<CardComponent>()!!
        card.name shouldBe "Phyrexian"

        // DFC component flipped to BACK.
        val dfc = container.get<DoubleFacedComponent>()!!
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // Counters survive the transform (they ride on the entity, not the face).
        val counters = container.get<CountersComponent>()!!
        counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3

        // Projected stats: back face is 0/0, three +1/+1 counters → 3/3.
        val projected = projector.project(driver.state)
        projected.getPower(tokenId) shouldBe 3
        projected.getToughness(tokenId) shouldBe 3
    }
})
