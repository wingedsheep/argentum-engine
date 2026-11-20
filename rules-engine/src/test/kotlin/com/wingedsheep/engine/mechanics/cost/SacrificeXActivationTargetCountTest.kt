package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An activated ability whose variable sacrifice cost announces X (CR 601.2b) and whose targets are
 * "X target …" (`targets(…, exactly = X)`). The target choice raised after the sacrifice pick is
 * sized from that X through `DynamicTargetCount` — before, its static minimum read 0 and the choice
 * was skipped, leaving an X-target ability with no targets.
 */
class SacrificeXActivationTargetCountTest : FunSpec({

    val reanimator = card("Test Stray Reanimator") {
        manaCost = "{B}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(
                Costs.Tap,
                Costs.SacrificePermanents(GameObjectFilter.Creature, minCount = 0),
            )
            targets(TargetFilter.CreatureInYourGraveyard, exactly = DynamicAmounts.xValue())
            effect = Effects.ForEachTarget(Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.ContextTarget(0)))
        }
    }

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(reanimator))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val abilityId = reanimator.activatedAbilities.single().id

    test("the target choice after sacrificing two creatures is exactly two") {
        val d = driver()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Stray Reanimator")
        val a = d.putCreatureOnBattlefield(you, "Savannah Lions")
        val b = d.putCreatureOnBattlefield(you, "Goblin Guide")
        d.putCardInGraveyard(you, "Centaur Courser")
        d.putCardInGraveyard(you, "Force of Nature")
        d.putCardInGraveyard(you, "Phantom Warrior")

        d.submit(ActivateAbility(you, source, abilityId))
        val pick = d.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(pick.id, listOf(a, b)))

        val targets = d.pendingDecision
        targets.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().minTargets shouldBe 2
        targets.targetRequirements.single().maxTargets shouldBe 2
        targets.legalTargets[0]!!.size shouldBe 3
    }

    test("X larger than the legal targets refuses the activation before anything is sacrificed") {
        val d = driver()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Stray Reanimator")
        val a = d.putCreatureOnBattlefield(you, "Savannah Lions")
        val b = d.putCreatureOnBattlefield(you, "Goblin Guide")
        d.putCardInGraveyard(you, "Centaur Courser")

        d.submit(ActivateAbility(you, source, abilityId))
        val pick = d.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(pick.id, listOf(a, b))).error.shouldNotBeNull()

        val battlefield = d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD))
        battlefield.contains(a) shouldBe true
        battlefield.contains(b) shouldBe true
    }

    test("sacrificing nothing announces X = 0 and asks for no target") {
        val d = driver()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Stray Reanimator")
        d.putCreatureOnBattlefield(you, "Savannah Lions")
        d.putCardInGraveyard(you, "Centaur Courser")

        d.submit(ActivateAbility(you, source, abilityId))
        val pick = d.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(pick.id, emptyList()))
        (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        d.state.stack.size shouldBe 1
    }
})
