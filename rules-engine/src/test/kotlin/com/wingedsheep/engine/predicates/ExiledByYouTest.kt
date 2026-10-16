package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
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
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.ExiledByYou` — "if it was exiled by an ability you controlled" (Evelyn, the
 * Covetous). The exile roads stamp the controller of the exiling effect on the card's
 * [ExiledFromZoneComponent]; the predicate matches that player only, and only while the card is
 * still in exile.
 */
class ExiledByYouTest : FunSpec({

    // "If a card would be put into a graveyard from anywhere, exile it instead."
    val graveyardHole = card("Exiled-By Test Graveyard Hole") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Any, to = Zone.GRAVEYARD),
            )
        )
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(graveyardHole)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val exiledByYou = GameObjectFilter.Any.exiledByYou()

    fun GameTestDriver.matchesFor(playerId: EntityId, cardId: EntityId) =
        services.predicateEvaluator.matches(
            state, state.projectedState, cardId, exiledByYou,
            PredicateContext.fromEffectContext(EffectContext(sourceId = null, controllerId = playerId))
        )

    fun GameTestDriver.run(effect: Effect, controllerId: EntityId, target: EntityId? = null) {
        val result = services.effectExecutorRegistry.execute(
            state, effect,
            EffectContext(
                sourceId = null, controllerId = controllerId,
                targets = listOfNotNull(target?.let { ChosenTarget.Permanent(it) })
            )
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.exiledBy(cardId: EntityId) = state.getEntity(cardId)?.get<ExiledFromZoneComponent>()?.exiledBy

    test("a single-target exile is attributed to the effect's controller, not the card's owner") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.run(Effects.Exile(EffectTarget.ContextTarget(0)), controllerId = opp, target = bears)

        d.state.getExile(me).contains(bears) shouldBe true
        d.exiledBy(bears) shouldBe opp
        d.matchesFor(opp, bears) shouldBe true
        d.matchesFor(me, bears) shouldBe false
    }

    test("a pipeline exile of each player's top card is attributed to its controller for every card") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val myTop = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val theirTop = d.putCardOnTopOfLibrary(opp, "Grizzly Bears")

        d.run(
            Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
                move(top, CardDestination.ToZone(Zone.EXILE))
            },
            controllerId = me
        )

        d.exiledBy(myTop) shouldBe me
        d.exiledBy(theirTop) shouldBe me
        d.matchesFor(me, theirTop) shouldBe true
        d.matchesFor(opp, theirTop) shouldBe false
    }

    test("a move redirected into exile isn't attributed to whoever was moving it elsewhere") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, graveyardHole.name)
        val bears = d.putCardInHand(me, "Grizzly Bears")

        // The opponent's pipeline would put it into my graveyard — the one road that carries an
        // exiling controller — and my enchantment exiles it instead.
        d.run(
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(zone = Zone.HAND, player = Player.Each, filter = GameObjectFilter.Any))
                move(hand, CardDestination.ToZone(Zone.GRAVEYARD))
            },
            controllerId = opp
        )

        d.state.getExile(me).contains(bears) shouldBe true
        d.matchesFor(opp, bears) shouldBe false
    }

    test("the predicate stops matching once the card leaves exile") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.run(Effects.Exile(EffectTarget.ContextTarget(0)), controllerId = me, target = bears)
        d.matchesFor(me, bears) shouldBe true

        d.run(
            Effects.Pipeline {
                val exiled = gather(CardSource.FromZone(zone = Zone.EXILE, player = Player.You, filter = GameObjectFilter.Any))
                move(exiled, CardDestination.ToZone(Zone.HAND))
            },
            controllerId = me
        )

        d.state.getHand(me).contains(bears) shouldBe true
        d.matchesFor(me, bears) shouldBe false
    }
})
