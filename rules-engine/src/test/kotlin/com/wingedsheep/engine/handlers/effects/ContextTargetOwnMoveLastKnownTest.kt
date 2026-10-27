package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A chosen target ([EffectTarget.ContextTarget]) that an earlier instruction of the same
 * resolution moved off the battlefield is read as it last existed there (CR 608.2h) — the
 * snapshot the move recorded in [com.wingedsheep.engine.handlers.EffectContext.departedPermanents]
 * ([LkiPolicy.LIVE_THEN_OWN_MOVE_LKI]). A target that left the battlefield some *other* time is a
 * new object (CR 400.7) and is read where it is now.
 */
class ContextTargetOwnMoveLastKnownTest : FunSpec({

    /** "Return target creature to its owner's hand. You gain life equal to its power." */
    val bounceDrain = card("Test Bounce Drain") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(creature) then
                GainLifeEffect(DynamicAmounts.powerOf(creature), EffectTarget.Controller)
        }
    }

    /** "Destroy target creature. You gain life equal to its power." */
    val destroyDrain = card("Test Destroy Drain") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature) then
                GainLifeEffect(DynamicAmounts.powerOf(creature), EffectTarget.Controller)
        }
    }

    /** "Return target creature card from your graveyard to your hand. You gain life equal to its power." */
    val raiseDrain = card("Test Raise Drain") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.ReturnToHand(creature) then
                GainLifeEffect(DynamicAmounts.powerOf(creature), EffectTarget.Controller)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(bounceDrain, destroyDrain, raiseDrain))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bearsWithCounters(owner: EntityId): EntityId {
        val bears = putCreatureOnBattlefield(owner, "Grizzly Bears")
        replaceState(state.updateEntity(bears) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })
        return bears
    }

    fun GameTestDriver.cast(player: EntityId, name: String, target: EntityId) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.BLACK, 1)
        giveMana(player, Color.BLUE, 1)
        castSpell(player, spell, listOf(target)).error shouldBe null
        bothPass()
    }

    test("a target this resolution bounced is read as it last existed on the battlefield") {
        val d = driver()
        val p = d.activePlayer!!
        val bears = d.bearsWithCounters(d.getOpponent(p))
        d.cast(p, "Test Bounce Drain", bears)
        withClue("the returned Bears had two +1/+1 counters: power 4, not the card's printed 2") {
            d.getLifeTotal(p) shouldBe 24
        }
    }

    test("a target this resolution destroyed is read as it last existed on the battlefield") {
        val d = driver()
        val p = d.activePlayer!!
        val bears = d.bearsWithCounters(d.getOpponent(p))
        d.cast(p, "Test Destroy Drain", bears)
        d.getLifeTotal(p) shouldBe 24
    }

    test("a target card that left the battlefield in an earlier event is read as the card it is now") {
        val d = driver()
        val p = d.activePlayer!!
        val bears = d.bearsWithCounters(p)
        d.cast(p, "Test Destroy Drain", bears)
        d.getLifeTotal(p) shouldBe 24
        val raise = d.putCardInHand(p, "Test Raise Drain")
        d.giveMana(p, Color.BLACK, 1)
        d.castSpellWithTargets(p, raise, listOf(ChosenTarget.Card(bears, p, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()
        withClue("the Bears card in the graveyard is a new object (CR 400.7): its printed power 2") {
            d.getLifeTotal(p) shouldBe 26
        }
    }
})
