package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.json.Json

class TrainingObservationTest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(PortalSet.cards)
        registry.register(PortalSet.basicLands)
        return registry
    }

    fun simpleDeck() = Deck.of("Mountain" to 17, "Raging Goblin" to 3)

    fun newEnv(): GameEnvironment {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return env
    }

    val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    test("forced play exposes scoped ordinary actions as executable decision templates") {
        val env = newEnv()
        val player = env.state.activePlayerId!!
        val land = env.state.getHand(player).first { id ->
            env.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name == "Mountain"
        }
        val services = com.wingedsheep.engine.core.EngineServices(env.cardRegistry)
        val forced = services.effectExecutorRegistry.execute(env.state, Effects.ForcePlay("chosen"),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = player,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf("chosen" to listOf(land)))))
        env.restore(forced.state, env.playerIds)
        val result = ObservationBuilder(env.cardRegistry).build(env.state, player, env.legalActions())
        val observation = result.observation as TrainingObservation
        observation.pendingDecision!!.kind shouldBe PendingDecisionKind.PLAY_CARD
        observation.pendingDecision!!.subjectEntityId shouldBe land
        observation.legalActions.shouldNotBeEmpty()
        observation.legalActions.all { it.isDecisionOption }.shouldBeTrue()
        val observer = env.playerIds.first { it != player }
        val hidden = ObservationBuilder(env.cardRegistry).build(env.state, observer, env.legalActions())
        val hiddenObservation = hidden.observation as TrainingObservation
        hiddenObservation.pendingDecision!!.subjectEntityId shouldBe null
        hiddenObservation.legalActions.shouldBeEmpty()
        hidden.registry.size shouldBe 0
        val response = result.registry.decisionResponses.single().second as com.wingedsheep.engine.core.PlayCardResponse
        (response.action as com.wingedsheep.engine.core.PlayLand).cardId shouldBe land
        env.step(com.wingedsheep.engine.core.SubmitDecision(player, response))
        env.lastRejection shouldBe null
        env.state.getBattlefield(player).contains(land).shouldBeTrue()
    }

    test("observation includes all basic state fields and round-trips through JSON") {
        val env = newEnv()
        val perspective = env.playerIds[0]
        val result = ObservationBuilder(env.cardRegistry).build(env.state, perspective, env.legalActions())

        val obs = result.observation as TrainingObservation
        obs.perspectivePlayerId shouldBe perspective
        obs.players.size shouldBe 2
        obs.agentToAct.shouldNotBeNull()
        obs.terminated.shouldBeFalse()
        obs.schemaHash shouldBe SchemaHash.CURRENT
        obs.stateDigest shouldMatch Regex("[0-9a-f]{64}")
        obs.legalActions.shouldNotBeEmpty()

        // Every player-keyed zone is present; stack has its own ordered field.
        obs.zones.size shouldBe 2 * (Zone.entries.size - 1)

        val encoded = json.encodeToString(TrainingObservation.serializer(), obs)
        val decoded = json.decodeFromString(TrainingObservation.serializer(), encoded)
        decoded shouldBe obs
    }

    test("per-player zones have an explicit complete schema order") {
        TRAINING_OBSERVATION_ZONE_ORDER.shouldContainExactly(
            Zone.HAND,
            Zone.LIBRARY,
            Zone.GRAVEYARD,
            Zone.EXILE,
            Zone.BATTLEFIELD,
            Zone.COMMAND,
            Zone.SIDEBOARD,
        )
        // Every engine zone is classified in the contract itself, so a new one cannot be absorbed
        // by relaxing this test — it has to be named either per-player or explicitly not.
        TRAINING_OBSERVATION_ZONE_ORDER.toSet() + NON_PLAYER_KEYED_ZONES shouldBe
            Zone.entries.toSet()
        TRAINING_OBSERVATION_ZONE_ORDER.toSet() shouldNotContainAnyOf NON_PLAYER_KEYED_ZONES

        val env = newEnv()
        val perspective = env.playerIds[0]
        val observation = ObservationBuilder(env.cardRegistry)
            .build(env.state, perspective, env.legalActions())
            .observation as TrainingObservation

        observation.zones
            .filter { it.ownerId == perspective }
            .map { it.zoneType }
            .shouldContainExactly(TRAINING_OBSERVATION_ZONE_ORDER)
    }

    test("an actionId from the observation resolves to a steppable action") {
        val env = newEnv()
        val perspective = env.playerIds[0]
        val result = ObservationBuilder(env.cardRegistry).build(env.state, perspective, env.legalActions())

        val passView = result.observation.legalActions.first { it.description.contains("Pass", ignoreCase = true) || it.kind.contains("Pass", ignoreCase = true) }
        val resolved = result.registry.resolve(passView.actionId)
        resolved.shouldNotBe(ResolvedAction.Unknown)

        val legal = resolved as ResolvedAction.Legal
        (legal.action is PassPriority).shouldBeTrue()

        val stepResult = env.step(legal.action)
        stepResult.state.shouldNotBeNull()
        env.stepCount shouldBeGreaterThan 0
    }

    test("opponent hand is hidden by default, visible when revealAll=true") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]

        val masked = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions())
            .observation as TrainingObservation
        val myHand = masked.zones.single { it.ownerId == me && it.zoneType == Zone.HAND }
        val theirHand = masked.zones.single { it.ownerId == opponent && it.zoneType == Zone.HAND }
        myHand.hidden.shouldBeFalse()
        myHand.cards.size shouldBe myHand.size
        theirHand.hidden.shouldBeTrue()
        theirHand.cards.size shouldBe 0
        theirHand.size shouldBeGreaterThan 0

        // Every library is hidden regardless of perspective.
        masked.zones.filter { it.zoneType == Zone.LIBRARY }.forEach { it.hidden.shouldBeTrue() }

        val revealed = ObservationBuilder(env.cardRegistry).build(
            env.state,
            me,
            env.legalActions(),
            revealAll = true,
        )
            .observation as TrainingObservation
        val theirHandRevealed = revealed.zones.single { it.ownerId == opponent && it.zoneType == Zone.HAND }
        theirHandRevealed.hidden.shouldBeFalse()
        theirHandRevealed.cards.size shouldBe theirHandRevealed.size
    }

    test("oracle text is serialized for cards in visible zones") {
        // Raging Goblin's printed text is "Haste" — that's what the agent
        // needs to see to know the creature can attack the turn it enters.
        val env = newEnv()
        val me = env.playerIds[0]

        val obs = ObservationBuilder(env.cardRegistry).build(
            env.state,
            me,
            env.legalActions(),
        ).observation as TrainingObservation
        val myHand = obs.zones.single { it.ownerId == me && it.zoneType == Zone.HAND }

        // At least one card should be in the opening hand; every card there
        // is fully visible to the perspective player including its oracle text.
        myHand.cards.shouldNotBeEmpty()
        val rages = myHand.cards.filter { it.name == "Raging Goblin" }
        if (rages.isNotEmpty()) {
            rages.first().oracleText shouldContain "Haste"
        }
        // Round-trip through JSON — oracle text must survive.
        val encoded = json.encodeToString(TrainingObservation.serializer(), obs)
        val decoded = json.decodeFromString(TrainingObservation.serializer(), encoded)
        decoded.zones.flatMap { it.cards }.forEach { c ->
            // Every serialized card either has empty oracle text or preserves it.
            c.oracleText shouldBe (obs.zones.flatMap { it.cards }.first { it.entityId == c.entityId }.oracleText)
        }
    }

    test("a card revealed to this perspective is visible inside its still-hidden zone") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]
        val opponentHand = env.state.getZone(ZoneKey(opponent, Zone.HAND))
        opponentHand.size shouldBeGreaterThan 1

        fun observe(state: GameState) =
            ObservationBuilder(env.cardRegistry).build(state, me, env.legalActions()).observation as TrainingObservation

        fun revealing(cardId: EntityId): GameState =
            env.state.withEntity(
                cardId,
                env.state.getEntity(cardId)!!.with(RevealedToComponent.to(me))
            )

        fun opponentHandView(obs: TrainingObservation) =
            obs.zones.first { it.ownerId == opponent && it.zoneType == Zone.HAND }

        opponentHandView(observe(env.state)).cards.shouldBeEmpty()

        val revealed = opponentHandView(observe(revealing(opponentHand[0])))
        // Peeking at one card does not unhide the zone: the rest of the hand is still unknown,
        // so `size` stays the true count while `cards` holds only what this player has seen.
        revealed.hidden.shouldBeTrue()
        revealed.size shouldBe opponentHand.size
        revealed.cards.map { it.entityId } shouldBe listOf(opponentHand[0])
    }

    test("stateDigest distinguishes which card is known inside a hidden zone") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]
        val opponentHand = env.state.getZone(ZoneKey(opponent, Zone.HAND))

        fun digestRevealing(cardId: EntityId): String =
            ObservationBuilder(env.cardRegistry).build(
                env.state.withEntity(
                    cardId,
                    env.state.getEntity(cardId)!!.with(RevealedToComponent.to(me))
                ),
                me,
                env.legalActions()
            ).observation.stateDigest

        val nothingKnown = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions())
            .observation.stateDigest

        digestRevealing(opponentHand[0]) shouldNotBe nothingKnown
        digestRevealing(opponentHand[0]) shouldNotBe digestRevealing(opponentHand[1])
    }

    test("a spell on the stack reports its chosen targets, and they reach the digest") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]
        val handKey = ZoneKey(me, Zone.HAND)
        val spellId = env.state.getZone(handKey).first()

        fun casting(target: EntityId): GameState {
            val hand = env.state.getZone(handKey)
            return env.state
                .copy(zones = env.state.zones + (handKey to hand - spellId), stack = listOf(spellId))
                .withEntity(
                    spellId,
                    env.state.getEntity(spellId)!!
                        // A real cast stamps the caster as controller on the way to the stack.
                        .with(ControllerComponent(me))
                        .with(TargetsComponent(listOf(ChosenTarget.Player(target))))
                )
        }

        fun observe(state: GameState) =
            ObservationBuilder(env.cardRegistry).build(state, me, env.legalActions()).observation as TrainingObservation

        val atOpponent = observe(casting(opponent))
        val item = atOpponent.stack.single()
        item.name.shouldNotBeEmpty()
        item.controllerId shouldBe me
        item.targets shouldBe listOf(opponent)

        // Same spell, other target: an agent that cannot tell these apart cannot search.
        atOpponent.stateDigest shouldNotBe observe(casting(me)).stateDigest
    }

    test("a triggered ability on the stack reports its source and controller") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]
        val abilityId = EntityId.of("trigger-on-stack")
        val state = env.state
            .withEntity(
                abilityId,
                ComponentContainer.of(
                    TriggeredAbilityOnStackComponent(
                        sourceId = EntityId.of("source"),
                        sourceName = "Raging Goblin",
                        controllerId = opponent,
                        effect = Effects.DrawCards(1),
                        description = "Draw a card."
                    )
                )
            )
            .copy(stack = listOf(abilityId))

        val item = (ObservationBuilder(env.cardRegistry).build(state, me, env.legalActions())
            .observation as TrainingObservation).stack.single()

        // An ability is its own entity with no CardComponent, so it used to arrive unnamed,
        // uncontrolled and classified OTHER — three fields the digest now hashes.
        item.kind shouldBe StackItemKind.TRIGGERED_ABILITY
        item.controllerId shouldBe opponent
        item.name shouldBe "Raging Goblin"
    }

    test("stateDigest reflects every observable field of a stack item") {
        val env = newEnv()
        val me = env.playerIds[0]
        val opponent = env.playerIds[1]
        val base = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions())
            .observation as TrainingObservation

        val stackItem = StackItemView(
            entityId = EntityId.of("stack-item"),
            controllerId = me,
            name = "Visible Spell",
            kind = StackItemKind.SPELL,
            oracleText = "Visible rules text",
            targets = listOf(EntityId.of("target-a")),
        )
        val baseline = StateDigest.compute(base.copy(stack = listOf(stackItem)))

        listOf(
            stackItem.copy(controllerId = opponent),
            stackItem.copy(name = "Different Visible Spell"),
            stackItem.copy(kind = StackItemKind.TRIGGERED_ABILITY),
            stackItem.copy(oracleText = "Different visible rules text"),
            stackItem.copy(targets = listOf(EntityId.of("target-b"))),
        ).forEach { changed ->
            StateDigest.compute(base.copy(stack = listOf(changed))) shouldNotBe baseline
        }
    }

    test("stateDigest is unambiguous when a card name contains the encoding's delimiters") {
        val env = newEnv()
        val me = env.playerIds[0]
        val base = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions())
            .observation as TrainingObservation

        fun item(id: String, name: String) = StackItemView(
            entityId = EntityId.of(id),
            controllerId = null,
            name = name,
            kind = StackItemKind.SPELL,
        )

        // Card names are author-supplied, so a name can spell the digest's own field separators.
        // This one reproduces the two-item encoding exactly, and collides with it unless the
        // free-form fields are length-prefixed.
        val smuggled = item("s", "N:text=:targets=|S[1]=s2:ctl=null:kind=SPELL:name=M")
        val twoItems = listOf(item("s", "N"), item("s2", "M"))

        StateDigest.compute(base.copy(stack = listOf(smuggled))) shouldNotBe
            StateDigest.compute(base.copy(stack = twoItems))
    }

    test("stateDigest is stable for equivalent observations and changes when state changes") {
        val env = newEnv()
        val me = env.playerIds[0]

        val a = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions()).observation
        val b = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions()).observation
        a.stateDigest shouldBe b.stateDigest

        // Advance a step and verify the digest changes.
        val pass = env.legalActions().first { it.action is PassPriority }
        env.step(pass.action)
        val c = ObservationBuilder(env.cardRegistry).build(env.state, me, env.legalActions()).observation
        c.stateDigest shouldNotBe a.stateDigest
    }
})
