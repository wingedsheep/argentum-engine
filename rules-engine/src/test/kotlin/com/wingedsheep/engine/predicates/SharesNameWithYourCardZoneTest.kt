package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CardPredicate.SharesNameWithYourCard`'s zone axis — "an instant or sorcery card with the same
 * name as a card in your graveyard" (Frostpyre Arcanist). Off the battlefield the compared set is
 * the evaluating player's *own* zone; a face-down card there has no name (CR 708.2a) and shares
 * none (CR 201.2a); the battlefield default still compares against permanents you control.
 */
class SharesNameWithYourCardZoneTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val me = EntityId.generate()
    val opponent = EntityId.generate()

    fun card(name: String, owner: EntityId, type: CardType = CardType.INSTANT) = ComponentContainer()
        .with(CardComponent(
            cardDefinitionId = name,
            name = name,
            manaCost = ManaCost(emptyList()),
            typeLine = TypeLine(cardTypes = setOf(type)),
            ownerId = owner,
        ))
        .with(OwnerComponent(owner))
        .with(ControllerComponent(owner))

    fun GameState.put(id: EntityId, container: ComponentContainer, owner: EntityId, zone: Zone) =
        withEntity(id, container).addToZone(ZoneKey(owner, zone), id)

    val base = GameState()
        .withEntity(me, ComponentContainer())
        .withEntity(opponent, ComponentContainer())

    val boltInLibrary = EntityId.generate()
    val shockInLibrary = EntityId.generate()
    val opponentsBoltInLibrary = EntityId.generate()
    val boltInGraveyard = EntityId.generate()
    val shockInTheirGraveyard = EntityId.generate()

    val state = base
        .put(boltInLibrary, card("Lightning Bolt", me), me, Zone.LIBRARY)
        .put(shockInLibrary, card("Shock", me), me, Zone.LIBRARY)
        .put(opponentsBoltInLibrary, card("Lightning Bolt", opponent), opponent, Zone.LIBRARY)
        .put(boltInGraveyard, card("Lightning Bolt", me), me, Zone.GRAVEYARD)
        .put(shockInTheirGraveyard, card("Shock", opponent), opponent, Zone.GRAVEYARD)

    val inMyGraveyard = GameObjectFilter.InstantOrSorcery.sharingNameWithCardInYour(Zone.GRAVEYARD)
    val ctx = PredicateContext(controllerId = me)

    test("matches a card named like a card in your own graveyard, not an opponent's") {
        evaluator.matches(state, state.projectedState, boltInLibrary, inMyGraveyard, ctx) shouldBe true
        evaluator.matches(state, state.projectedState, shockInLibrary, inMyGraveyard, ctx) shouldBe false
    }

    test("the compared zone is the evaluating player's, whoever owns the candidate") {
        evaluator.matches(state, state.projectedState, opponentsBoltInLibrary, inMyGraveyard, ctx) shouldBe true
        val theirs = PredicateContext(controllerId = opponent)
        evaluator.matches(state, state.projectedState, shockInLibrary, inMyGraveyard, theirs) shouldBe true
        evaluator.matches(state, state.projectedState, boltInLibrary, inMyGraveyard, theirs) shouldBe false
    }

    test("excludeSelf keeps a graveyard card from matching itself") {
        val another = GameObjectFilter(cardPredicates = listOf(
            CardPredicate.SharesNameWithYourCard(GameObjectFilter.Any, excludeSelf = true, zone = Zone.GRAVEYARD)
        ))
        evaluator.matches(state, state.projectedState, boltInGraveyard, another, ctx) shouldBe false
        val second = EntityId.generate()
        val withTwo = state.put(second, card("Lightning Bolt", me), me, Zone.GRAVEYARD)
        evaluator.matches(withTwo, withTwo.projectedState, boltInGraveyard, another, ctx) shouldBe true
    }

    test("a face-down card in the compared zone has no name") {
        val foretold = EntityId.generate()
        val shockFaceDown = card("Shock", me).with(FaceDownComponent)
        val withExile = state.put(foretold, shockFaceDown, me, Zone.EXILE)
        val inMyExile = GameObjectFilter.InstantOrSorcery.sharingNameWithCardInYour(Zone.EXILE)
        evaluator.matches(withExile, withExile.projectedState, shockInLibrary, inMyExile, ctx) shouldBe false
        val faceUp = withExile.withEntity(foretold, card("Shock", me))
        evaluator.matches(faceUp, faceUp.projectedState, shockInLibrary, inMyExile, ctx) shouldBe true
    }

    test("the battlefield default still compares permanents you control, not cards elsewhere") {
        val bolt = EntityId.generate()
        val onBattlefield = GameObjectFilter.Any.sharingNameWithPermanentYouControl(GameObjectFilter.Any)
        evaluator.matches(state, state.projectedState, boltInLibrary, onBattlefield, ctx) shouldBe false
        val withPermanent = state.put(bolt, card("Lightning Bolt", me, CardType.ARTIFACT), me, Zone.BATTLEFIELD)
        evaluator.matches(withPermanent, withPermanent.projectedState, boltInLibrary, onBattlefield, ctx) shouldBe true
    }

    test("the description names the zone") {
        CardPredicate.SharesNameWithYourCard(GameObjectFilter.Any, zone = Zone.GRAVEYARD).description shouldBe
            "with the same name as a card in your graveyard"
        CardPredicate.SharesNameWithYourCard(GameObjectFilter.Permanent).description shouldBe
            "with the same name as a permanent you control"
    }
})
