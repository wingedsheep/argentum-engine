package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainActivatedAbilityOn
import com.wingedsheep.engine.legalactions.support.shouldNotContainActivatedAbilityOn
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.mtg.sets.definitions.khans.cards.ArchersParapet
import com.wingedsheep.mtg.sets.definitions.khans.cards.DisownedAncestor
import com.wingedsheep.mtg.sets.definitions.khans.cards.RakshasaDeathdealer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.ActivatedAbilityEnumerator].
 *
 * Paths covered:
 * - Mana-cost-only activated abilities (Rakshasa Deathdealer's +2/+2 and regenerate)
 * - Composite Tap+Mana costs (Archers' Parapet): tapped source, summoning sickness
 *   — both yield unaffordable entries (not dropped) because cost-pay-check sets
 *   costAffordable=false rather than `continue`-skipping inside the Composite branch
 * - Sorcery-speed restriction (Disowned Ancestor's outlast at upkeep)
 * - Activation restrictions (Weathered Wayfarer's "only if an opponent controls
 *   more lands")
 * - Unaffordable cost emits greyed entry
 * - Face-down suppression (Rule 707.2)
 * - Opponent's permanents not surfaced for me
 *
 * Deferred to a follow-up phase: "any player may activate" (e.g. Lethal Vapors),
 * granted-ability grants from static effects, planeswalker loyalty limits,
 * class level-up, X-variable costs, tap-attached-creature (Equipment-style),
 * convoke on activated abilities, target-requirement paths (auto-select,
 * self-target auto-select, ordinary target).
 */
class ActivatedAbilityEnumeratorTest : FunSpec({

    /** Battlefield entity id for the P1 permanent matching [name]. */
    fun entityOnBattlefield(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getBattlefield(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    // -------------------------------------------------------------------------
    context("Mana-cost-only activated abilities (own permanent)") {

        test("Rakshasa Deathdealer in play with {B}{G} available surfaces BOTH activated abilities") {
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            val view = driver.enumerateFor(driver.player1)

            view shouldContainActivatedAbilityOn rakshasaId
            // Two activated abilities: +2/+2 and regenerate. Both pure {B}{G}.
            val abilities = view.activatedAbilityActionsFor(rakshasaId)
            abilities shouldHaveSize 2
            abilities.forEach { it.affordable shouldBe true }
            abilities.forEach { it.manaCostString shouldBe "{B}{G}" }
        }

        test("without the right mana, the ability is still emitted but marked unaffordable") {
            // Only a Forest — can't pay {B}.
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(rakshasaId)

            abilities shouldHaveSize 2
            abilities.forEach { it.affordable shouldBe false }
        }

        test("no source on battlefield produces no activated-ability actions for that card") {
            // Put only lands down; Rakshasa lives in the graveyard — not a battlefield permanent.
            val driver = setupP1(
                battlefield = listOf("Swamp", "Forest"),
                graveyard = listOf("Rakshasa Deathdealer"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )

            driver.enumerateFor(driver.player1)
                .filter {
                    (it.action as? ActivateAbility)
                        ?.let { act -> driver.game.state.getEntity(act.sourceId)?.get<CardComponent>()?.name == "Rakshasa Deathdealer" }
                        ?: false
                }.shouldBeEmpty()
        }

        test("opponent controls Rakshasa — I do NOT see its activated abilities in my list") {
            // P1 has lands but no Rakshasa; Rakshasa lives on P1's battlefield — we enumerate as P2.
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            driver.enumerateFor(driver.player2) shouldNotContainActivatedAbilityOn rakshasaId
        }
    }

    // -------------------------------------------------------------------------
    context("Composite Tap+Mana cost (Archers' Parapet)") {

        test("untapped source with mana and no summoning sickness surfaces the ability") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe true
        }

        test("source already tapped — ability emitted as unaffordable (Composite path)") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")
            // Tap the Parapet.
            val tapped = driver.game.state.getEntity(parapetId)!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(parapetId, tapped))

            // Composite cost path: Tap sub-cost fails → costCanBePaid=false →
            // the ability still emits as a greyed-out (affordable=false) entry.
            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe false
        }

        test("source with summoning sickness and no haste — ability emitted as unaffordable") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")
            // Simulate a just-played creature with summoning sickness.
            val sick = driver.game.state.getEntity(parapetId)!!.with(SummoningSicknessComponent)
            driver.game.replaceState(driver.game.state.withEntity(parapetId, sick))

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    context("Sorcery-speed timing (Disowned Ancestor's Outlast)") {

        test("Outlast is enumerated on the active player's main phase") {
            val driver = setupP1(
                battlefield = listOf("Disowned Ancestor", "Swamp", "Forest"),
                extraSetCards = listOf(DisownedAncestor)
            )
            val ancestorId = entityOnBattlefield(driver, "Disowned Ancestor")

            driver.enumerateFor(driver.player1) shouldContainActivatedAbilityOn ancestorId
        }

        test("Outlast is NOT enumerated on the upkeep step (sorcery-speed only)") {
            val driver = setupP1(
                battlefield = listOf("Disowned Ancestor", "Swamp", "Forest"),
                extraSetCards = listOf(DisownedAncestor),
                atStep = Step.UPKEEP
            )
            val ancestorId = entityOnBattlefield(driver, "Disowned Ancestor")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn ancestorId
        }
    }

    // -------------------------------------------------------------------------
    context("Activation restrictions (Weathered Wayfarer's OnlyIfCondition)") {

        test("opponent controls no more lands than me — restriction blocks the ability") {
            // P1 battlefield: Wayfarer + 1 Plains (1 land). P2 runs a pure-Forest
            // deck, so P2 has 0 lands on battlefield → opponent does NOT control
            // more lands. Restriction not met → ability is NOT enumerated.
            val driver = setupP1(
                battlefield = listOf("Weathered Wayfarer", "Plains")
            )
            val wayfarerId = entityOnBattlefield(driver, "Weathered Wayfarer")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn wayfarerId
        }

        test("opponent controls more lands than me — restriction met, ability surfaces") {
            // Bring P2 up to 2 Forests on battlefield — more than my 0 lands.
            val driver = setupP1(
                battlefield = listOf("Weathered Wayfarer")
            )
            val wayfarerId = entityOnBattlefield(driver, "Weathered Wayfarer")

            // Surgically move 2 Forests from P2's library to P2's battlefield.
            var state = driver.game.state
            val p2LibKey = com.wingedsheep.engine.state.ZoneKey(driver.player2, com.wingedsheep.sdk.core.Zone.LIBRARY)
            val p2BattlefieldKey = com.wingedsheep.engine.state.ZoneKey(driver.player2, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
            repeat(2) {
                val forestId = state.getZone(p2LibKey).first { id ->
                    state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                }
                state = state.moveToZone(forestId, p2LibKey, p2BattlefieldKey)
            }
            driver.game.replaceState(state)

            driver.enumerateFor(driver.player1) shouldContainActivatedAbilityOn wayfarerId
        }
    }

    // -------------------------------------------------------------------------
    context("Face-down suppression (Rule 707.2)") {

        test("a face-down permanent produces no activated abilities") {
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")
            // Flip Rakshasa face-down (simulating morph).
            val hidden = driver.game.state.getEntity(rakshasaId)!!.with(FaceDownComponent)
            driver.game.replaceState(driver.game.state.withEntity(rakshasaId, hidden))

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn rakshasaId
        }
    }

})
