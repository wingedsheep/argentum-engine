package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.WasDealtDamageThisTurnBy(sourceFilter)` — "a creature dealt damage this turn by a
 * source you controlled" (Etching of Kumano). Read off the damaged creature's own record of its
 * damage sources, each frozen as it stood at damage time, both by the predicate evaluator and by the
 * zone-change redirect path that answers "…would die, exile it instead".
 */
class WasDealtDamageThisTurnByTest : FunSpec({

    // "If a creature dealt damage this turn by a source you controlled would die, exile it instead."
    val etching = card("Damage-History Test Etching") {
        manaCost = "{R}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.wasDealtDamageThisTurnBy(GameObjectFilter.Any.youControl()),
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD,
                ),
            )
        )
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(etching)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val filter = GameObjectFilter.Creature.wasDealtDamageThisTurnBy(GameObjectFilter.Any.youControl())

    fun GameTestDriver.matchesFor(playerId: EntityId, cardId: EntityId, f: GameObjectFilter = filter) =
        services.predicateEvaluator.matches(
            state, state.projectedState, cardId, f,
            PredicateContext.fromEffectContext(EffectContext(sourceId = null, controllerId = playerId))
        )

    fun GameTestDriver.run(effect: Effect, controllerId: EntityId, sourceId: EntityId?, target: EntityId) {
        val result = services.effectExecutorRegistry.execute(
            state, effect,
            EffectContext(
                sourceId = sourceId, controllerId = controllerId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.ping(by: EntityId, source: EntityId, target: EntityId) =
        run(Effects.DealDamage(1, EffectTarget.ContextTarget(0)), by, source, target)

    fun GameTestDriver.destroy(by: EntityId, target: EntityId) =
        run(Effects.Destroy(EffectTarget.ContextTarget(0)), by, null, target)

    test("matches only for the player who controlled the damaging source") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mySource = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        withClue("undamaged") { d.matchesFor(me, victim) shouldBe false }
        d.ping(me, mySource, victim)

        d.matchesFor(me, victim) shouldBe true
        d.matchesFor(opp, victim) shouldBe false
    }

    test("a creature damaged by a source you controlled is exiled when something else kills it") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, etching.name)
        val mySource = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.ping(me, mySource, victim)
        // The killer is the opponent's own effect: how it dies doesn't matter, only who damaged it.
        d.destroy(opp, victim)

        d.state.getExile(opp).contains(victim) shouldBe true
        d.state.getGraveyard(opp).contains(victim) shouldBe false
    }

    test("a creature damaged only by an opponent's source dies normally") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, etching.name)
        val theirSource = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(me, "Centaur Courser")

        d.ping(opp, theirSource, victim)
        d.destroy(me, victim)

        d.state.getGraveyard(me).contains(victim) shouldBe true
    }

    test("an undamaged creature dies normally") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, etching.name)
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.destroy(me, victim)

        d.state.getGraveyard(opp).contains(victim) shouldBe true
    }

    test("the damaging source needn't still be on the battlefield") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, etching.name)
        val mySource = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.ping(me, mySource, victim)
        d.destroy(me, mySource)
        withClue("the source is gone (and, undamaged by my sources, went to the graveyard)") {
            d.state.getGraveyard(me).contains(mySource) shouldBe true
        }
        d.destroy(opp, victim)

        d.state.getExile(opp).contains(victim) shouldBe true
    }

    test("damage you dealt to your own creature counts too") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, etching.name)
        val mySource = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(me, "Centaur Courser")

        d.ping(me, mySource, victim)
        d.destroy(me, victim)

        d.state.getExile(me).contains(victim) shouldBe true
    }

    test("the source is judged by who controlled it when it dealt the damage, not who controls it now") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, etching.name)
        val mySource = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")

        d.ping(me, mySource, victim)
        d.run(Effects.GainControl(EffectTarget.ContextTarget(0)), opp, null, mySource)
        withClue("the opponent now controls the source") {
            d.state.projectedState.getController(mySource) shouldBe opp
        }

        d.matchesFor(me, victim) shouldBe true
        d.matchesFor(opp, victim) shouldBe false
        d.destroy(opp, victim)
        d.state.getExile(opp).contains(victim) shouldBe true
    }

    test("the source filter reads the source's damage-time type and subtypes, and fails closed past them") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        d.ping(me, bears, victim)

        d.matchesFor(me, victim, GameObjectFilter.Creature.wasDealtDamageThisTurnBy(
            GameObjectFilter.Creature.youControl().withSubtype("Bear"))) shouldBe true
        d.matchesFor(me, victim, GameObjectFilter.Creature.wasDealtDamageThisTurnBy(
            GameObjectFilter.Creature.withSubtype("Spider"))) shouldBe false
        d.matchesFor(opp, victim, GameObjectFilter.Creature.wasDealtDamageThisTurnBy(
            GameObjectFilter.Any.opponentControls())) shouldBe true
        withClue("a predicate the damage-time record can't answer never matches") {
            d.matchesFor(me, victim, GameObjectFilter.Creature.wasDealtDamageThisTurnBy(
                GameObjectFilter.Any.tapped())) shouldBe false
        }
    }
})
