package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phase 1 of the PayCost-payment unification (see backlog/paycost-payment-unification.md):
 * the shared [CostPaymentService] that owns affordability + payment for all ten [PayCost] variants.
 *
 * The service is not yet wired into any consumer, so these tests drive it directly: build a board,
 * call [CostPaymentService.canAfford] / [CostPaymentService.pay], then submit the decision through the
 * real [com.wingedsheep.engine.core.ActionProcessor] so the [com.wingedsheep.engine.core.CostPaymentContinuation]
 * resumes exactly as it will in production.
 */
class CostPaymentServiceTest : ScenarioTestBase() {

    private fun bfCardByName(state: GameState, playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {

        // -----------------------------------------------------------------------------------------
        // Mana
        // -----------------------------------------------------------------------------------------

        test("Mana: canAfford follows available mana; paying taps a source") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide") // the cost's source ({R} creature)
                .withLandsOnBattlefield(1, "Forest", 1)
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val forest = bfCardByName(game.state, game.player1Id, "Forest")

            service.canAfford(game.state, game.player1Id, Costs.pay.Mana(ManaCost.parse("{G}")), source).shouldBeTrue()
            service.canAfford(game.state, game.player1Id, Costs.pay.Mana(ManaCost.parse("{U}")), source).shouldBeFalse()

            val pending = service.pay(game.state, game.player1Id, Costs.pay.Mana(ManaCost.parse("{G}")), source)
            pending.shouldBeInstanceOf<PaymentResult.Pending>()
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, true))

            game.state.getEntity(forest)!!.has<TappedComponent>().shouldBeTrue()
        }

        test("Mana: declining leaves the source untapped and runs onDeclined") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val forest = bfCardByName(game.state, game.player1Id, "Forest")

            val pending = service.pay(
                game.state, game.player1Id, Costs.pay.Mana(ManaCost.parse("{G}")), source,
                CostPaymentContext(onDeclined = Effects.DrawCards(1))
            ) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, false))

            game.state.getEntity(forest)!!.has<TappedComponent>().shouldBeFalse()
            game.state.getHand(game.player1Id).size shouldBe 1 // drew from onDeclined
        }

        // -----------------------------------------------------------------------------------------
        // OwnManaCost
        // -----------------------------------------------------------------------------------------

        test("OwnManaCost: resolves to the source's printed cost and pays it") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide") // printed cost {R}
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val mountain = bfCardByName(game.state, game.player1Id, "Mountain")

            service.canAfford(game.state, game.player1Id, PayCost.OwnManaCost, source).shouldBeTrue()

            val pending = service.pay(game.state, game.player1Id, PayCost.OwnManaCost, source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, true))

            game.state.getEntity(mountain)!!.has<TappedComponent>().shouldBeTrue()
        }

        test("OwnManaCost: unaffordable without the right mana") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")

            service.canAfford(game.state, game.player1Id, PayCost.OwnManaCost, source).shouldBeFalse()
            service.pay(game.state, game.player1Id, PayCost.OwnManaCost, source)
                .shouldBeInstanceOf<PaymentResult.Unaffordable>()
        }

        // -----------------------------------------------------------------------------------------
        // PayLife (CR 119.4)
        // -----------------------------------------------------------------------------------------

        test("PayLife: canAfford uses CR 119.4 (life >= amount); paying deducts life") {
            val game = scenario().withPlayers().withLifeTotal(1, 5)
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")

            service.canAfford(game.state, game.player1Id, Costs.pay.PayLife(5), source).shouldBeTrue() // exactly enough
            service.canAfford(game.state, game.player1Id, Costs.pay.PayLife(6), source).shouldBeFalse()

            val pending = service.pay(game.state, game.player1Id, Costs.pay.PayLife(3), source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, true))

            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 2
        }

        test("PayLife: declining keeps life unchanged") {
            val game = scenario().withPlayers().withLifeTotal(1, 5)
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")

            val pending = service.pay(game.state, game.player1Id, Costs.pay.PayLife(3), source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, false))

            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 5
        }

        // -----------------------------------------------------------------------------------------
        // Discard
        // -----------------------------------------------------------------------------------------

        test("Discard: selecting the card moves it to the graveyard; declining keeps it in hand") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getHand(game.player1Id).first()
            val cost = Costs.pay.Discard(GameObjectFilter.Any, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()

            // Paid
            val paid = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = paid.state
            game.submitDecision(CardsSelectedResponse(paid.pendingDecision.id, listOf(card)))
            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)) shouldContain card
            game.state.getHand(game.player1Id).shouldBe(emptyList())
        }

        test("Discard: declining keeps the card in hand") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getHand(game.player1Id).first()

            val pending = service.pay(game.state, game.player1Id, Costs.pay.Discard(GameObjectFilter.Any, 1), source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, emptyList()))
            game.state.getHand(game.player1Id) shouldContain card
        }

        test("Discard: unaffordable with an empty hand") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Goblin Guide").build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            service.canAfford(game.state, game.player1Id, Costs.pay.Discard(GameObjectFilter.Any, 1), source).shouldBeFalse()
            service.pay(game.state, game.player1Id, Costs.pay.Discard(GameObjectFilter.Any, 1), source)
                .shouldBeInstanceOf<PaymentResult.Unaffordable>()
        }

        test("Discard (random): paying discards a card at random") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getHand(game.player1Id).first()

            val pending = service.pay(game.state, game.player1Id, Costs.pay.Discard(GameObjectFilter.Any, 1, random = true), source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(YesNoResponse(pending.pendingDecision.id, true))
            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)) shouldContain card
        }

        // -----------------------------------------------------------------------------------------
        // Exile
        // -----------------------------------------------------------------------------------------

        test("Exile: from graveyard moves the chosen card to exile") {
            val game = scenario().withPlayers()
                .withCardInGraveyard(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).first()
            val cost = Costs.pay.Exile(GameObjectFilter.Any, Zone.GRAVEYARD, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()
            val pending = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(card)))
            game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE)) shouldContain card
        }

        // -----------------------------------------------------------------------------------------
        // RevealCard
        // -----------------------------------------------------------------------------------------

        test("RevealCard: paying reveals the card but leaves it in hand") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getHand(game.player1Id).first()
            val cost = Costs.pay.RevealCard(GameObjectFilter.Any, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()
            val pending = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = pending.state
            val result = game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(card)))

            game.state.getHand(game.player1Id) shouldContain card // stays in hand
            result.events.any { it is CardsRevealedEvent }.shouldBeTrue()
        }

        // -----------------------------------------------------------------------------------------
        // Sacrifice
        // -----------------------------------------------------------------------------------------

        test("Sacrifice: excludes the source; paying sacrifices the chosen permanent") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide") // source — must be excluded
                .withCardOnBattlefield(1, "Savannah Lions") // the fodder
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val fodder = bfCardByName(game.state, game.player1Id, "Savannah Lions")
            val cost = Costs.pay.Sacrifice(GameObjectFilter.Any, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()
            val pending = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(fodder)))

            game.state.getBattlefield(game.player1Id) shouldContain source // source untouched
            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)) shouldContain fodder
        }

        test("Sacrifice: unaffordable when only the source is on the battlefield") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Goblin Guide").build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            service.canAfford(game.state, game.player1Id, Costs.pay.Sacrifice(GameObjectFilter.Any, 1), source).shouldBeFalse()
        }

        // -----------------------------------------------------------------------------------------
        // ReturnToHand
        // -----------------------------------------------------------------------------------------

        test("ReturnToHand: paying returns the chosen permanent to hand") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Savannah Lions")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val fodder = bfCardByName(game.state, game.player1Id, "Savannah Lions")
            val cost = Costs.pay.ReturnToHand(GameObjectFilter.Any, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()
            val pending = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(fodder)))

            game.state.getHand(game.player1Id) shouldContain fodder
            game.state.getBattlefield(game.player1Id).contains(fodder).shouldBeFalse()
        }

        // -----------------------------------------------------------------------------------------
        // Tap
        // -----------------------------------------------------------------------------------------

        test("Tap: paying taps the chosen untapped permanent; tapped ones aren't eligible") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Savannah Lions") // untapped fodder
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val fodder = bfCardByName(game.state, game.player1Id, "Savannah Lions")
            val cost = Costs.pay.Tap(GameObjectFilter.Any, 1)

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()
            val pending = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(fodder)))

            game.state.getEntity(fodder)!!.has<TappedComponent>().shouldBeTrue()
        }

        test("Tap: unaffordable when every permanent, including the source, is already tapped") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide", tapped = true)
                .withCardOnBattlefield(1, "Savannah Lions", tapped = true)
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            service.canAfford(game.state, game.player1Id, Costs.pay.Tap(GameObjectFilter.Any, 1), source).shouldBeFalse()
        }

        test("Tap: the source itself is an eligible candidate when the cost doesn't say 'another'") {
            // "Tap an untapped permanent you control" (Command Bridge, Sunshot Militia's cost
            // shape) has no "other" — the untapped source may pay its own cost.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Savannah Lions", tapped = true)
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            service.canAfford(game.state, game.player1Id, Costs.pay.Tap(GameObjectFilter.Any, 1), source).shouldBeTrue()
        }

        // -----------------------------------------------------------------------------------------
        // Choice (recursion: an option that itself needs input)
        // -----------------------------------------------------------------------------------------

        test("Choice: picking a sub-cost recurses into it and pays it") {
            val game = scenario().withPlayers().withLifeTotal(1, 10)
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val card = game.state.getHand(game.player1Id).first()
            val cost = Costs.pay.Choice(listOf(Costs.pay.PayLife(3), Costs.pay.Discard(GameObjectFilter.Any, 1)))

            service.canAfford(game.state, game.player1Id, cost, source).shouldBeTrue()

            // Pick option index 1 (discard) → recurse into the discard selection.
            val choice = service.pay(game.state, game.player1Id, cost, source) as PaymentResult.Pending
            game.state = choice.state
            game.submitDecision(OptionChosenResponse(choice.pendingDecision.id, 1))
            // The recursion left a fresh card-selection decision pending.
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            game.submitDecision(CardsSelectedResponse(game.state.pendingDecision!!.id, listOf(card)))

            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)) shouldContain card
            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 10 // life path untouched
        }

        test("Choice: the trailing 'Don't pay' option declines and runs onDeclined") {
            val game = scenario().withPlayers().withLifeTotal(1, 10)
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardOnBattlefield(1, "Goblin Guide")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val cost = Costs.pay.Choice(listOf(Costs.pay.PayLife(3), Costs.pay.Discard(GameObjectFilter.Any, 1)))

            val pending = service.pay(
                game.state, game.player1Id, cost, source,
                CostPaymentContext(onDeclined = Effects.DrawCards(1))
            ) as PaymentResult.Pending
            game.state = pending.state
            // Two affordable options + the trailing "Don't pay" at index 2.
            game.submitDecision(OptionChosenResponse(pending.pendingDecision.id, 2))

            game.state.getEntity(game.player1Id)!!.get<LifeTotalComponent>()!!.life shouldBe 10
            game.state.getHand(game.player1Id).size shouldBe 2 // original Forest + drawn card
        }

        // -----------------------------------------------------------------------------------------
        // Follow-up effects
        // -----------------------------------------------------------------------------------------

        test("onPaid runs after a successful payment") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Goblin Guide")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardInLibrary(1, "Forest")
                .build()
            val service = CostPaymentService(EngineServices(cardRegistry))
            val source = bfCardByName(game.state, game.player1Id, "Goblin Guide")
            val fodder = bfCardByName(game.state, game.player1Id, "Savannah Lions")

            val pending = service.pay(
                game.state, game.player1Id, Costs.pay.Sacrifice(GameObjectFilter.Any, 1), source,
                CostPaymentContext(onPaid = Effects.DrawCards(1))
            ) as PaymentResult.Pending
            game.state = pending.state
            game.submitDecision(CardsSelectedResponse(pending.pendingDecision.id, listOf(fodder)))

            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)) shouldContain fodder
            game.state.getHand(game.player1Id).size shouldBe 1 // onPaid drew a card
        }

        // -----------------------------------------------------------------------------------------
        // Serialization — the continuation must round-trip (it rides GameState across a pause).
        // -----------------------------------------------------------------------------------------

        test("CostPaymentContinuation round-trips through the engine serializers module") {
            val json = kotlinx.serialization.json.Json {
                serializersModule = com.wingedsheep.engine.core.engineSerializersModule
                encodeDefaults = true
            }
            val original: com.wingedsheep.engine.core.ContinuationFrame =
                com.wingedsheep.engine.core.CostPaymentContinuation(
                    decisionId = "d1",
                    payerId = EntityId.of("player-1"),
                    sourceId = EntityId.of("src"),
                    sourceName = "Goblin Guide",
                    cost = Costs.pay.Choice(listOf(Costs.pay.PayLife(3), Costs.pay.Discard(GameObjectFilter.Any, 1))),
                    onPaid = Effects.DrawCards(1),
                    onDeclined = Effects.GainLife(2)
                )
            val encoded = json.encodeToString(com.wingedsheep.engine.core.ContinuationFrame.serializer(), original)
            val decoded = json.decodeFromString(com.wingedsheep.engine.core.ContinuationFrame.serializer(), encoded)
            decoded shouldBe original
        }
    }
}
