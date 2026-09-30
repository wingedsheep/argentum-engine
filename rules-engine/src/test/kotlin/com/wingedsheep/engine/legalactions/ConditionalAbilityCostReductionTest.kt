package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.ActivatedAbility.conditionalCostReduction] — "This ability costs
 * [reduction] less to activate if [condition]" (Kami of Jealous Thirst).
 *
 * The reduction is a whole mana cost subtracted pip-wise (CR 118.7): a colored pip removes a
 * matching pip, and an unmatched one spills onto generic. These tests pin that arithmetic and the
 * condition gate on the enumerated (offered) cost; the Kami's scenario test pins the paid cost.
 */
class ConditionalAbilityCostReductionTest : FunSpec({

    // "{3}{B}{B}: You gain 1 life. This ability costs {R}{B} less while you control a Bear."
    val engine = card("Test Thirst Engine") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{3}{B}{B}")
            effect = Effects.GainLife(1)
            costsLessIf("{R}{B}", Conditions.ControlCreatureOfType(Subtype("Bear")))
        }
    }

    // "{1}{B}, {T}: You gain 1 life. This ability costs {1}{B} less while you control a Bear."
    val tapEngine = card("Test Tap Thirst Engine") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}{B}"), AbilityCost.Tap)
            effect = Effects.GainLife(1)
            costsLessIf("{1}{B}", Conditions.ControlCreatureOfType(Subtype("Bear")))
        }
    }

    fun permanent(driver: EnumerationTestDriver, name: String): EntityId =
        driver.game.state.getBattlefield().first {
            driver.game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    fun offeredCost(driver: EnumerationTestDriver, name: String): String? =
        driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(permanent(driver, name)).single().manaCostString

    test("condition false — the printed {3}{B}{B} is offered") {
        val driver = setupP1(
            battlefield = listOf("Test Thirst Engine"),
            extraSetCards = listOf(engine),
            atStep = Step.PRECOMBAT_MAIN
        )
        offeredCost(driver, "Test Thirst Engine") shouldBe "{3}{B}{B}"
    }

    test("condition true — {B} removes a {B}, the unmatched {R} spills onto generic") {
        val driver = setupP1(
            battlefield = listOf("Test Thirst Engine", "Grizzly Bears"),
            extraSetCards = listOf(engine),
            atStep = Step.PRECOMBAT_MAIN
        )
        withClue("CR 118.7: {3}{B}{B} − {R}{B} = {2}{B}") {
            offeredCost(driver, "Test Thirst Engine") shouldBe "{2}{B}"
        }
    }

    test("a composite cost loses its mana entirely and keeps the tap") {
        val driver = setupP1(
            battlefield = listOf("Test Tap Thirst Engine", "Grizzly Bears"),
            extraSetCards = listOf(tapEngine),
            atStep = Step.PRECOMBAT_MAIN
        )
        val actions = driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(permanent(driver, "Test Tap Thirst Engine"))
        actions.size shouldBe 1
        withClue("no lands at all, yet the {T}-only remainder is payable") {
            actions.single().affordable shouldBe true
        }
    }
})
