package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 611.2b: if a "for as long as …" duration ends before the effect would begin, the effect does
 * nothing — "it doesn't start and immediately stop again". For a control change that means no
 * control change at all: no [ControlChangedEvent], no summoning sickness stamped on the target, no
 * floating effect left behind (Dragonlord Silumgar ruling: "If Dragonlord Silumgar ceases to be
 * under your control before its ability resolves, you won't gain control of the creature or
 * planeswalker at all").
 *
 * Every case puts a steal ability on the stack, ends its duration in response, then resolves it.
 */
class GainControlDurationEndedBeforeResolutionTest : FunSpec({

    val YouControlStealer = card("You Control Stealer") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        power = 2
        toughness = 2
        oracleText = "{T}: Gain control of target creature for as long as you control this creature."
        activatedAbility {
            cost = Costs.Tap
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t, Duration.WhileYouControlSource("You Control Stealer"))
        }
    }

    val TappedStealer = card("Tapped Stealer") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        power = 2
        toughness = 2
        oracleText = "{T}: Gain control of target creature for as long as this creature remains tapped."
        activatedAbility {
            cost = Costs.Tap
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t, Duration.WhileSourceTapped("Tapped Stealer"))
        }
    }

    val InstantThreaten = card("Instant Threaten") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Gain control of target creature until end of turn."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GainControl(t, Duration.EndOfTurn)
        }
    }

    val InstantFlicker = card("Instant Flicker") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Exile target creature you control, then return that card to the battlefield under your control."
        spell {
            val t = target(TargetFilter.CreatureYouControl)
            effect = Effects.Exile(t) then Effects.PutOntoBattlefieldUnderYourControl(t)
        }
    }

    val Bear = CardDefinition.creature("Steal Target Bear", ManaCost.parse("{1}{G}"), emptySet(), 2, 2)

    data class Setup(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val target: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(YouControlStealer, TappedStealer, InstantThreaten, InstantFlicker, Bear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val target = driver.putCreatureOnBattlefield(opponent, "Steal Target Bear")
        driver.removeSummoningSickness(target)
        return Setup(driver, you, opponent, target)
    }

    /** Activates [stealerCard]'s steal on [target]; the ability is left on the stack. */
    fun GameTestDriver.activateSteal(owner: EntityId, stealerCard: CardDefinition, target: EntityId): EntityId {
        val stealer = putCreatureOnBattlefield(owner, stealerCard.name)
        removeSummoningSickness(stealer)
        submit(
            ActivateAbility(
                playerId = owner,
                sourceId = stealer,
                abilityId = stealerCard.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).outcome shouldBe Outcome.Done
        return stealer
    }

    fun Setup.assertNothingHappened() {
        driver.state.projectedState.getController(target) shouldBe opponent
        driver.state.getEntity(target)!!.has<SummoningSicknessComponent>() shouldBe false
        driver.events.none { it is ControlChangedEvent && it.permanentId == target } shouldBe true
        driver.state.floatingEffects.none { it.effect.layer == Layer.CONTROL && target in it.effect.affectedEntities } shouldBe true
    }

    test("baseline: the steal resolves while you still control the source") {
        val s = setup()
        s.driver.activateSteal(s.you, YouControlStealer, s.target)
        s.driver.bothPass()
        s.driver.state.projectedState.getController(s.target) shouldBe s.you
        s.driver.state.getEntity(s.target)!!.has<SummoningSicknessComponent>() shouldBe true
    }

    test("source left the battlefield before resolution: no control change") {
        val s = setup()
        val stealer = s.driver.activateSteal(s.you, YouControlStealer, s.target)
        s.driver.moveToGraveyard(stealer)
        s.driver.bothPass()
        s.assertNothingHappened()
    }

    test("another player gained control of the source before resolution: no control change") {
        val s = setup()
        val stealer = s.driver.activateSteal(s.you, YouControlStealer, s.target)
        s.driver.passPriority(s.you)
        val threaten = s.driver.putCardInHand(s.opponent, "Instant Threaten")
        s.driver.castSpell(s.opponent, threaten, listOf(stealer)).outcome shouldBe Outcome.Done
        s.driver.bothPass() // Instant Threaten resolves
        s.driver.state.projectedState.getController(stealer) shouldBe s.opponent
        s.driver.bothPass() // the steal resolves
        s.assertNothingHappened()
    }

    test("source flickered before resolution: the returned permanent is a new object, so no control change") {
        val s = setup()
        val stealer = s.driver.activateSteal(s.you, YouControlStealer, s.target)
        val flicker = s.driver.putCardInHand(s.you, "Instant Flicker")
        s.driver.castSpell(s.you, flicker, listOf(stealer)).outcome shouldBe Outcome.Done
        s.driver.bothPass() // Instant Flicker resolves
        (s.driver.findPermanent(s.you, "You Control Stealer") != null) shouldBe true
        s.driver.bothPass() // the steal resolves
        s.assertNothingHappened()
    }

    test("source-tapped duration: a source untapped before resolution ends it first") {
        val s = setup()
        val stealer = s.driver.activateSteal(s.you, TappedStealer, s.target)
        s.driver.untapPermanent(stealer)
        s.driver.bothPass()
        s.assertNothingHappened()
    }
})
