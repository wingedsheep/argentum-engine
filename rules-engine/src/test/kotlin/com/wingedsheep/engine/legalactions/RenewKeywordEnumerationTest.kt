package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainActivatedAbilityOn
import com.wingedsheep.engine.legalactions.support.shouldNotContainActivatedAbilityOn
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.renew
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain

/**
 * Enumeration tests for the Renew keyword (Tarkir: Dragonstorm, Sultai).
 *
 * Renew composes a graveyard-activated ability: "[cost], Exile this card from your
 * graveyard: [effect]. Activate only as a sorcery." The `renew(cost) { … }` DSL helper
 * fixes `activateFromZone = GRAVEYARD` and `timing = SorcerySpeed`; the
 * [enumerators.ZoneActivatedAbilityEnumerator] must surface it only at sorcery speed.
 *
 * These tests assert the sorcery-timing gate (the engine change) and the standard
 * cost-affordability `continue`.
 */
class RenewKeywordEnumerationTest : FunSpec({

    /** A {1}{G} 2/2 creature with "Renew — {1}{G}, Exile this card from your graveyard: Draw a card." */
    val renewingOoze = card("Renewing Ooze") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Ooze"
        power = 2
        toughness = 2
        renew("{1}{G}") {
            effect = Effects.DrawCards(1)
        }
    }

    fun entityInGraveyard(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getGraveyard(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    test("at precombat main with mana — renew ability is enumerated from the graveyard") {
        val driver = setupP1(
            battlefield = listOf("Forest", "Forest"),
            graveyard = listOf("Renewing Ooze"),
            extraSetCards = listOf(renewingOoze),
            atStep = Step.PRECOMBAT_MAIN
        )
        val oozeId = entityInGraveyard(driver, "Renewing Ooze")

        val view = driver.enumerateFor(driver.player1)
        view shouldContainActivatedAbilityOn oozeId
        val abilities = view.activatedAbilityActionsFor(oozeId)
        abilities shouldHaveSize 1
        abilities.single().description.shouldContain("Renew")
    }

    test("at upkeep — NOT enumerated (\"Activate only as a sorcery\" gate)") {
        val driver = setupP1(
            battlefield = listOf("Forest", "Forest"),
            graveyard = listOf("Renewing Ooze"),
            extraSetCards = listOf(renewingOoze),
            atStep = Step.UPKEEP
        )
        val oozeId = entityInGraveyard(driver, "Renewing Ooze")

        driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn oozeId
    }

    test("at precombat main with insufficient mana — NOT enumerated (cost unpayable)") {
        val driver = setupP1(
            battlefield = listOf("Forest"), // only one source; renew costs {1}{G}
            graveyard = listOf("Renewing Ooze"),
            extraSetCards = listOf(renewingOoze),
            atStep = Step.PRECOMBAT_MAIN
        )
        val oozeId = entityInGraveyard(driver, "Renewing Ooze")

        driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn oozeId
    }
})
