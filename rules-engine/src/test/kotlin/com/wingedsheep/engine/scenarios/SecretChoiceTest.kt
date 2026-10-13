package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class SecretChoiceTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun start(d: GameTestDriver, effect: Effect, context: EffectContext = EffectContext(null, d.player1)) {
        val result = d.services.effectExecutorRegistry.execute(d.state, effect, context)
        result.error shouldBe null
        d.replaceState(result.state)
    }
    fun number(d: GameTestDriver, n: Int): ExecutionResult {
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        val q = d.pendingDecision!!
        return d.submitDecision(q.playerId, NumberChosenResponse(q.id, n)).also { it.error shouldBe null }
    }
    fun ballots(d: GameTestDriver, a: EntityId?, b: EntityId?) {
        for (pick in listOf(a, b)) {
            d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
            val q = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            q.useTargetingUI shouldBe true
            d.submitDecision(q.playerId, CardsSelectedResponse(q.id, listOfNotNull(pick))).error shouldBe null
        }
    }
    fun voteEffect(then: Effect? = null) = SecretChoiceEffect(choice = SecretChoice.Entity("pool", storeMostChosen = "winners"), then = then)
    fun context(d: GameTestDriver, pool: List<EntityId>) = EffectContext(null, d.player1,
        pipeline = PipelineState(storedCollections = mapOf("pool" to pool)))
    val tapWinners = ForEachEffect(IterationSpace.Collection("winners"), Effects.Tap(EffectTarget.IterationEntity))

    test("number limits retain original X and reject out of range answers") {
        val d = driver()
        start(d, SecretChoiceEffect(choice = SecretChoice.Number(minValue = 1, maxValue = DynamicAmount.XValue)),
            EffectContext(null, d.player1, xValue = 3))
        val q = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        q.minValue shouldBe 1; q.maxValue shouldBe 3
        d.submitDecision(q.playerId, NumberChosenResponse(q.id, 4)).error?.isNotEmpty() shouldBe true
        number(d, 2)
        d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>().maxValue shouldBe 3
        number(d, 3)
    }
    test("matched zero answers run one original-controller branch and preserve original X") {
        val d = driver()
        start(d, SecretChoiceEffect(choice = SecretChoice.Number(
            highestEffect = Effects.LoseLife(9, EffectTarget.Controller),
            matchedEffect = Effects.GainLife(DynamicAmount.XValue, EffectTarget.Controller),
            unmatchedEffect = Effects.LoseLife(4, EffectTarget.Controller))), EffectContext(null, d.player1, xValue = 7))
        number(d, 0); number(d, 0)
        d.getLifeTotal(d.player1) shouldBe 27
        d.getLifeTotal(d.player2) shouldBe 20
    }
    test("nonmatching numbers run unmatched once and ranks ignore answers below the outcome floor") {
        val d = driver()
        start(d, SecretChoiceEffect(choice = SecretChoice.Number(
            highestEffect = Effects.LoseLife(DynamicAmount.XValue, EffectTarget.Controller),
            lowestEffect = Effects.GainLife(1, EffectTarget.Controller),
            unmatchedEffect = Effects.LoseLife(2, EffectTarget.Controller))))
        number(d, 0); number(d, 3)
        d.getLifeTotal(d.player1) shouldBe 18
        d.getLifeTotal(d.player2) shouldBe 18
    }
    test("all tied numeric groups run per participant and zero can qualify when explicitly configured") {
        val d = driver()
        start(d, SecretChoiceEffect(choice = SecretChoice.Number(outcomeMinValue = 0,
            highestEffect = Effects.LoseLife(1, EffectTarget.Controller),
            lowestEffect = Effects.LoseLife(1, EffectTarget.Controller),
            tiedEffect = Effects.LoseLife(1, EffectTarget.Controller))))
        number(d, 0); number(d, 0)
        d.getLifeTotal(d.player1) shouldBe 17; d.getLifeTotal(d.player2) shouldBe 17
    }
    test("numbers have no public description before the final answer and views never contain saved answers") {
        val d = driver()
        start(d, SecretChoiceEffect(choice = SecretChoice.Number()))
        val first = number(d, 73)
        first.events.filterIsInstance<DecisionSubmittedEvent>().mapNotNull { it.description } shouldBe emptyList()
        ClientEventTransformer.transform(first.events, d.player2, d.state).filterIsInstance<ClientEvent.DecisionMade>() shouldBe emptyList()
        val transformer = ClientStateTransformer(d.cardRegistry, predicateEvaluator = d.services.predicateEvaluator)
        for (spectator in listOf(false, true)) {
            val view = transformer.transform(d.state, d.player1, isSpectator = spectator)
            val encoded = json.encodeToString(view)
            encoded.contains("chosenNumbers") shouldBe false
            encoded.contains("\"numbers\"") shouldBe false
            encoded.contains("SecretChoiceContinuation") shouldBe false
        }
        val last = number(d, 11)
        last.events.filterIsInstance<DecisionSubmittedEvent>().mapNotNull { it.description }.size shouldBe 2
        ClientEventTransformer.transform(last.events, d.player2, d.state).filterIsInstance<ClientEvent.DecisionMade>().size shouldBe 2
    }
    test("participants deduplicate, rotate in multiplayer order, and skip eliminated players") {
        val d = driver()
        val third = EntityId.generate(); val lost = EntityId.generate()
        d.replaceState(d.state.copy(entities = d.state.entities + mapOf(third to d.state.getEntity(d.player1)!!,
            lost to d.state.getEntity(d.player1)!!.with(PlayerLostComponent(LossReason.CONCESSION))),
            turnOrder = listOf(d.player1, d.player2, third, lost), activePlayerId = d.player2))
        start(d, SecretChoiceEffect(participants = listOf(Player.Each, Player.You), choice = SecretChoice.Number()))
        d.pendingDecision!!.playerId shouldBe d.player2; number(d, 1)
        d.pendingDecision!!.playerId shouldBe third; number(d, 2)
        d.pendingDecision!!.playerId shouldBe d.player1; number(d, 3)
        d.pendingDecision shouldBe null
    }
    test("collected participants choose in APNAP order without duplicate or eliminated seats") {
        val d = driver()
        val lost = EntityId.generate()
        d.replaceState(d.state.copy(
            entities = d.state.entities + (lost to d.state.getEntity(d.player1)!!
                .with(PlayerLostComponent(LossReason.CONCESSION))),
            turnOrder = listOf(d.player1, d.player2, lost),
            activePlayerId = d.player2,
        ))
        start(d, SecretChoiceEffect(
            participants = listOf(Player.InCollection("participants"), Player.You),
            choice = SecretChoice.Number(matchedEffect = Effects.GainLife(2, EffectTarget.Controller)),
        ), EffectContext(null, d.player1, pipeline = PipelineState(storedCollections = mapOf(
            "participants" to listOf(d.player1, lost, d.player2, d.player2),
        ))))
        d.pendingDecision!!.playerId shouldBe d.player2
        number(d, 4)
        d.pendingDecision!!.playerId shouldBe d.player1
        number(d, 4)
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 22
        d.getLifeTotal(d.player2) shouldBe 20
    }
    test("entity tie stores all positive leaders and an enclosing sibling sees the winners") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        start(d, CompositeEffect(listOf(voteEffect(), tapWinners)), context(d, listOf(a, b)))
        ballots(d, a, b)
        d.state.getEntity(a)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(b)!!.has<TappedComponent>() shouldBe true
    }
    test("abstention never wins and empty pools finish without asking") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        start(d, voteEffect(tapWinners), context(d, listOf(a)))
        ballots(d, null, a)
        d.state.getEntity(a)!!.has<TappedComponent>() shouldBe true
        start(d, voteEffect(Effects.GainLife(1, EffectTarget.Controller)), context(d, emptyList()))
        d.pendingDecision shouldBe null; d.getLifeTotal(d.player1) shouldBe 21
    }
    test("secret entity submissions reveal together and hide face-down identities even when the source is gone") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(a) { it.with(FaceDownComponent) })
        start(d, voteEffect(Effects.GainLife(1, EffectTarget.Controller)),
            context(d, listOf(a)).copy(sourceId = EntityId.generate()))
        var q = d.pendingDecision!!
        val first = d.submitDecision(q.playerId, CardsSelectedResponse(q.id, listOf(a)))
        first.events.filterIsInstance<DecisionSubmittedEvent>().mapNotNull { it.description } shouldBe emptyList()
        q = d.pendingDecision!!
        val last = d.submitDecision(q.playerId, CardsSelectedResponse(q.id, emptyList()))
        last.error shouldBe null
        val revealed = last.events.filterIsInstance<DecisionSubmittedEvent>().mapNotNull { it.description }
        revealed.size shouldBe 2
        revealed.any { it.contains("Grizzly Bears") } shouldBe false
        d.getLifeTotal(d.player1) shouldBe 21
    }
    test("numeric reveal never exposes a face-down source name") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        start(d, SecretChoiceEffect(choice = SecretChoice.Number()), EffectContext(source, d.player1))
        number(d, 1)
        val final = number(d, 2)
        final.events.filterIsInstance<DecisionSubmittedEvent>().mapNotNull { it.description }
            .any { it.contains("Grizzly Bears") } shouldBe false
    }
    test("gathered creature ballots use projected characteristics") {
        val d = driver()
        val land = d.putPermanentOnBattlefield(d.player1, "Island")
        start(d, Effects.BecomeCreature(power = 2, toughness = 2), EffectContext(land, d.player1))
        start(d, CompositeEffect(listOf(
            GatherCardsEffect(CardSource.BattlefieldMatching(GameObjectFilter.Creature), "pool"),
            voteEffect(tapWinners))))
        val question = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        question.options shouldBe listOf(land)
        ballots(d, land, land)
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
    }
    test("a multiplayer majority publishes only the entity with most ballots") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        val third = EntityId.generate()
        d.replaceState(d.state.copy(entities = d.state.entities + (third to d.state.getEntity(d.player1)!!),
            turnOrder = listOf(d.player1, d.player2, third)))
        start(d, voteEffect(tapWinners), context(d, listOf(a, b)))
        for (pick in listOf(a, b, a)) {
            val q = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            d.submitDecision(q.playerId, CardsSelectedResponse(q.id, listOf(pick))).error shouldBe null
        }
        d.pendingDecision shouldBe null
        d.state.getEntity(a)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(b)!!.has<TappedComponent>() shouldBe false
    }
    test("mandatory secret entity choices reject abstention and objects outside the pool") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val outside = d.putPermanentOnBattlefield(d.player2, "Grizzly Bears")
        start(d, SecretChoiceEffect(choice = SecretChoice.Entity("pool", optional = false, storeMostChosen = "winners"),
            then = tapWinners), context(d, listOf(a)))
        val q = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        q.minSelections shouldBe 1
        d.submitDecision(q.playerId, CardsSelectedResponse(q.id, emptyList())).error?.isNotEmpty() shouldBe true
        d.pendingDecision!!.id shouldBe q.id
        d.submitDecision(q.playerId, CardsSelectedResponse(q.id, listOf(outside))).error?.isNotEmpty() shouldBe true
        d.pendingDecision!!.id shouldBe q.id
        d.submitDecision(q.playerId, CardsSelectedResponse(q.id, listOf(a, outside))).error?.isNotEmpty() shouldBe true
        ballots(d, a, a)
        d.pendingDecision shouldBe null
        d.state.getEntity(a)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(outside)!!.has<TappedComponent>() shouldBe false
    }
    test("all abstain stores an empty winner collection") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        start(d, voteEffect(tapWinners), context(d, listOf(a)))
        ballots(d, null, null)
        d.state.getEntity(a)!!.has<TappedComponent>() shouldBe false
    }
    test("a matched branch pause retains the secret-choice then and its enclosing sibling") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        start(d, CompositeEffect(listOf(SecretChoiceEffect(choice = SecretChoice.Number(
            matchedEffect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller)),
            then = Effects.GainLife(2, EffectTarget.Controller)), Effects.GainLife(3, EffectTarget.Controller))))
        number(d, 1); number(d, 1)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(a)).error shouldBe null
        d.pendingDecision shouldBe null; d.getLifeTotal(d.player1) shouldBe 25
    }
})
