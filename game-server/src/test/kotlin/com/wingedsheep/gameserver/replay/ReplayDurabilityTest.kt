package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.springframework.web.socket.WebSocketSession
import java.time.Instant

/**
 * The durability half of the replay format: what happens to a stored replay when the engine it was
 * recorded on is no longer the engine reading it.
 *
 * Cards in this engine are data the engine folds the recorded action stream through, so editing a
 * card rewrites the past — the single most common reason a stored replay stops replaying. These
 * tests pin the two defences: archived card definitions ([ReplayCardPin]), which make card edits
 * irrelevant, and position checkpoints ([ReplayFingerprint]), which catch the drift that pinning
 * can't cover instead of rendering a game nobody played.
 *
 * The lossless-round-trip guarantee itself lives in [CompactReplayReconstructionTest].
 */
class ReplayDurabilityTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** A stand-in for "a card whose implementation someone changes later". */
    private fun bear(power: Int, toughness: Int) = CardDefinition.creature(
        name = CARD,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = power,
        toughness = toughness,
    )

    /** Record a short real game whose decks contain [CARD]. */
    private fun recordGame(registry: CardRegistry): CompactReplay {
        val session = GameSession(cardRegistry = registry, maxPlayers = 2)
        val p1 = EntityId.of("pin-p1")
        val p2 = EntityId.of("pin-p2")
        val deck = mapOf("Forest" to 30, CARD to 10)
        session.addPlayer(PlayerSession(mockWs("pin-ws1"), p1, "Alice"), deck)
        session.addPlayer(PlayerSession(mockWs("pin-ws2"), p2, "Bob"), deck)
        session.startGame()
        session.keepHand(p1)
        session.keepHand(p2)
        repeat(40) {
            val state = session.getStateForTesting() ?: return@repeat
            if (state.gameOver) return@repeat
            state.priorityPlayerId?.let { session.executeAutoPass(it) }
        }

        return CompactReplay(
            gameId = session.sessionId,
            players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
            startedAt = Instant.now().toString(),
            endedAt = Instant.now().toString(),
            winnerName = null,
            setup = session.getReplaySetup().shouldNotBeNull(),
            actions = session.getRecordedActions(),
            yields = session.getReplayYields(),
            engineVersion = "test-build",
            pinnedCards = session.getPinnedCards(),
            checkpoints = session.getReplayCheckpoints(),
        )
    }

    init {
        test("a pinned definition shadows a card whose implementation changed since the recording") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val pinned = ReplayCardPin.capture(
                recording,
                setupWithDeck(mapOf("Forest" to 1, CARD to 1)),
            )
            pinned.shouldNotBeEmpty()

            // The corpus moves on: same card name, different implementation.
            val live = CardRegistry(parent = cardRegistry).apply { register(bear(5, 5)) }
            live.requireCard(CARD).creatureStats?.power shouldBe CharacteristicValue.Fixed(5)

            val overlaid = ReplayCardPin.overlay(live, pinned)
            overlaid.requireCard(CARD).creatureStats?.power shouldBe CharacteristicValue.Fixed(2)
            // Cards the replay never pinned still resolve, through to the live corpus.
            overlaid.requireCard("Forest").name shouldBe "Forest"
        }

        test("pinning preserves ability ids, which recorded yields are matched by") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            // Grizzly Bears has no abilities; use a real printed card with one instead.
            val withAbility = recording.requireCard("Llanowar Elves")
            val abilityIds = withAbility.script.activatedAbilities.map { it.id }
            abilityIds.shouldNotBeEmpty()

            val pinned = ReplayCardPin.capture(
                recording,
                setupWithDeck(mapOf("Llanowar Elves" to 1)),
            )
            val restored = ReplayCardPin.overlay(cardRegistry, pinned).requireCard("Llanowar Elves")
            restored.script.activatedAbilities.map { it.id } shouldBe abilityIds
        }

        test("a recorded game re-simulates byte-identically after its cards are changed underneath it") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            replay.pinnedCards.shouldNotBeEmpty()

            val liveTruth = ReplayReconstructor(recording, null)
                .reconstructStateAt(replay, replay.actions.size).shouldNotBeNull()

            // Deploy: the card is now a 5/5. Every copy of it in both libraries is a different object.
            val changed = CardRegistry(parent = cardRegistry).apply { register(bear(5, 5)) }

            // Without the pin, reconstruction produces a game that was never played...
            val unpinned = ReplayReconstructor(changed, null)
                .reconstructStateAt(replay.copy(pinnedCards = emptyList()), replay.actions.size)
                .shouldNotBeNull()
            unpinned.entities shouldNotBe liveTruth.entities

            // ...with it, the recorded definitions win and the game comes back exactly.
            val pinned = ReplayReconstructor(changed, null)
                .reconstructStateAt(replay, replay.actions.size).shouldNotBeNull()
            pinned.entities shouldBe liveTruth.entities
            pinned.zones shouldBe liveTruth.zones
        }

        test("a faithful reconstruction with checkpoints reports EXACT and the full frame stream") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            replay.checkpoints.shouldNotBeEmpty()

            val result = ReplayReconstructor(recording, null).reconstruct(replay)
            result.fidelity shouldBe ReplayFidelity.EXACT
            result.frameCount shouldBe (1 + replay.actions.size)
            result.isComplete shouldBe true
        }

        test("a checkpoint that no longer matches stops the replay instead of rendering a wrong game") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            val first = replay.checkpoints.first()

            // Stand in for engine drift the action stream can't detect on its own: the actions still
            // apply, but the position they produce is not the one that was recorded.
            val drifted = replay.copy(
                checkpoints = listOf(first.copy(fingerprint = "0000000000000000")) +
                    replay.checkpoints.drop(1)
            )

            val result = ReplayReconstructor(recording, null).reconstruct(drifted)
            result.fidelity shouldBe ReplayFidelity.DIVERGED
            result.isComplete shouldBe false
            // The checkpoint is verified after the Nth action, so the last frame we can vouch
            // for is the one before it.
            result.divergedAtFrame shouldBe (first.afterActionCount - 1)
            result.divergenceReason.shouldNotBeNull()

            // And no scenario can be forked out of a position we can't vouch for.
            ReplayReconstructor(recording, null)
                .reconstructStateAt(drifted, replay.actions.size) shouldBe null
        }

        test("a replay that no longer re-simulates is served from the archive, not truncated") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            val drifted = replay.copy(
                checkpoints = listOf(replay.checkpoints.first().copy(fingerprint = "0000000000000000"))
            )

            val store = InMemoryReplayStore()
            store.save(StoredReplay(drifted, ReplayStatus.FINISHED, presentation = ARCHIVED_BODY))
            val service = ReplayService(store, ReplayReconstructor(recording, null), mockk(relaxed = true))

            val payload = service.viewerPayload(drifted.gameId).shouldNotBeNull()
            // The whole game, exactly as it was played — just not re-derived.
            payload.body shouldBe ARCHIVED_BODY
            payload.frameCount shouldBe drifted.frameCount
            payload.fidelity shouldBe ReplayFidelity.DIVERGED
            // ...but there is no game state behind those frames, so no scenario can be forked.
            payload.stateReproducible shouldBe false
            payload.degradedReason.shouldNotBeNull()
        }

        test("a diverged replay with no archive degrades to the truncated stream, flagged") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            val drifted = replay.copy(
                checkpoints = listOf(replay.checkpoints.first().copy(fingerprint = "0000000000000000"))
            )

            val store = InMemoryReplayStore()
            store.save(StoredReplay(drifted, ReplayStatus.FINISHED, presentation = null))
            val service = ReplayService(store, ReplayReconstructor(recording, null), mockk(relaxed = true))

            val payload = service.viewerPayload(drifted.gameId).shouldNotBeNull()
            payload.frameCount shouldBe replay.checkpoints.first().afterActionCount
            payload.fidelity shouldBe ReplayFidelity.DIVERGED
            payload.stateReproducible shouldBe false
            payload.degradedReason.shouldNotBeNull()
        }

        test("a faithful replay is served from the re-simulation, with scenario sharing intact") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)

            val store = InMemoryReplayStore()
            store.save(StoredReplay(replay, ReplayStatus.FINISHED, presentation = ARCHIVED_BODY))
            val service = ReplayService(store, ReplayReconstructor(recording, null), mockk(relaxed = true))

            val payload = service.viewerPayload(replay.gameId).shouldNotBeNull()
            payload.fidelity shouldBe ReplayFidelity.EXACT
            payload.stateReproducible shouldBe true
            payload.degradedReason shouldBe null
            // The archive exists but is not what we served — the live fold wins while it is honest.
            payload.body shouldNotBe ARCHIVED_BODY
        }

        test("the viewer body splices into a well-formed response, and a malformed archive fails loudly") {
            val payload = ReplayViewerPayload(
                body = """{"initialSnapshot":{"seq":1},"deltas":[]}""",
                frameCount = 1,
                fidelity = ReplayFidelity.EXACT,
            )

            // What PublicReplayController builds: its own metadata key plus the frames verbatim.
            val spliced = """{"metadata":{"gameId":"g"},${payload.bodyFields()}}"""
            Json.parseToJsonElement(spliced).jsonObject.keys shouldBe
                setOf("metadata", "initialSnapshot", "deltas")

            // An archive row that isn't a JSON object would otherwise splice into corrupt JSON that
            // only fails in the client. Fail here, where the cause is visible.
            shouldThrow<IllegalArgumentException> { payload.copy(body = "not json").bodyFields() }
        }

        test("a v1 record (no checkpoints) still reconstructs, reported as unverified") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording).copy(checkpoints = emptyList())

            val result = ReplayReconstructor(recording, null).reconstruct(replay)
            result.fidelity shouldBe ReplayFidelity.UNVERIFIED
            result.frameCount shouldBe (1 + replay.actions.size)
        }

        test("pins round-trip through their own write-once column encoding") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            replay.pinnedCards.shouldNotBeEmpty()

            // Stored separately from the blob, so the blob no longer carries them...
            val encodedPins = ReplayCodec.encodePins(replay.pinnedCards).shouldNotBeNull()
            ReplayCodec.decodePins(encodedPins) shouldBe replay.pinnedCards
            // ...and the two recombine into the record the reconstructor needs.
            val blob = ReplayCodec.encode(replay.copy(pinnedCards = emptyList()))
            ReplayCodec.decode(blob).copy(pinnedCards = ReplayCodec.decodePins(encodedPins)) shouldBe replay

            // An unpinned record leaves the column honestly empty rather than storing "[]".
            ReplayCodec.encodePins(emptyList()) shouldBe null
            ReplayCodec.decodePins(null) shouldBe emptyList()
        }

        test("the durable codec round-trips every v2 field") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val replay = recordGame(recording)
            ReplayCodec.decode(ReplayCodec.encode(replay)) shouldBe replay
        }

        test("a record written before v2 decodes with the new fields defaulted") {
            val recording = CardRegistry(parent = cardRegistry).apply { register(bear(2, 2)) }
            val v2 = recordGame(recording)
            // Strip the v2 fields the way a record written by the old build would have them absent.
            val v1 = v2.copy(
                version = 1,
                engineVersion = CompactReplay.UNKNOWN_VERSION,
                pinnedCards = emptyList(),
                checkpoints = emptyList(),
            )
            val decoded = ReplayCodec.decode(ReplayCodec.encode(v1))
            decoded.version shouldBe 1
            decoded.pinnedCards shouldBe emptyList()
            decoded.checkpoints shouldBe emptyList()
            decoded.actions shouldBe v2.actions
        }
    }

    /** Minimal setup shell for the pin-capture tests, which only read the decklists. */
    private fun setupWithDeck(deck: Map<String, Int>) = ReplaySetup(
        seed = 0L,
        format = com.wingedsheep.sdk.core.Format.Standard,
        attackMode = com.wingedsheep.sdk.core.AttackMode.MULTIPLE,
        players = listOf(
            ReplayPlayerSetup(
                playerId = "p",
                name = "P",
                deck = com.wingedsheep.sdk.model.Deck(
                    cards = deck.flatMap { (name, count) -> List(count) { name } }
                ),
            )
        ),
        seatRoster = emptyList(),
    )

    private companion object {
        const val CARD = "Replay Pin Bear"

        /** Stand-in for a stored frame archive — served verbatim, so its contents don't matter here. */
        const val ARCHIVED_BODY = """{"initialSnapshot":{},"deltas":[]}"""
    }
}
