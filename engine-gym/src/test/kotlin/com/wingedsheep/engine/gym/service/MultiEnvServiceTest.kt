package com.wingedsheep.engine.gym.service

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.gym.contract.ResolvedAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Behavioural tests for [MultiEnvService] — covers env lifecycle, stepping
 * (single + batched), action-ID resolution, fork/snapshot/restore and
 * disposal. Uses Portal for deterministic creature decks and Bloomburrow
 * for the sealed-deck path.
 */
class MultiEnvServiceTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.allCards)
        register(BloomburrowSet.allCards)
    }

    fun boosterGen(): BoosterGenerator = BoosterGenerator(
        mapOf(
            BloomburrowSet.SET_CODE to BoosterGenerator.SetConfig(
                setCode = BloomburrowSet.SET_CODE,
                setName = BloomburrowSet.SET_NAME,
                cards = BloomburrowSet.allCards,
                basicLands = BloomburrowSet.basicLands
            )
        )
    )

    fun simpleDeck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    fun twoPlayerConfig(perspective: Int = 0) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "Alice", deck = simpleDeck()),
            PlayerSpec(name = "Bob", deck = simpleDeck())
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = perspective
    )

    // =========================================================================
    // Lifecycle
    // =========================================================================

    test("create returns an envId and an opening observation") {
        val svc = MultiEnvService(registry())
        val created = svc.create(twoPlayerConfig())

        created.envId.value.shouldNotBe("")
        created.observation.observation.terminated.shouldBeFalse()
        created.observation.observation.players shouldHaveSize 2
        created.observation.observation.legalActions.shouldNotBeEmpty()
        svc.listEnvs() shouldContainAll setOf(created.envId)
    }

    test("observe returns the current state without advancing") {
        val svc = MultiEnvService(registry())
        val (envId, opening) = svc.create(twoPlayerConfig())

        val again = svc.observe(envId)
        again.observation.stateDigest shouldBe opening.observation.stateDigest
    }

    test("reset on an existing envId reinitializes the game") {
        val svc = MultiEnvService(registry())
        val (envId, _) = svc.create(twoPlayerConfig())

        // Advance one step so we have something to reset away from.
        val firstLegal = svc.observe(envId).registry.legalActions.first().second
        val legal = firstLegal
        val preSize = legal.action // sanity use
        preSize.shouldNotBeNull()
        svc.step(StepRequest(envId, actionId = 0))

        val after = svc.reset(envId, twoPlayerConfig())
        // After reset, the env should be back at turn 1 with legal actions.
        after.observation.turnNumber shouldBe 1
        after.observation.legalActions.shouldNotBeEmpty()
        // EnvId is preserved across reset.
        svc.listEnvs() shouldContainAll setOf(envId)
    }

    test("dispose removes envs and makes them unknown") {
        val svc = MultiEnvService(registry())
        val a = svc.create(twoPlayerConfig()).envId
        val b = svc.create(twoPlayerConfig()).envId

        svc.dispose(listOf(a))
        svc.listEnvs() shouldContainAll setOf(b)
        svc.listEnvs().contains(a).shouldBeFalse()

        shouldThrow<NoSuchElementException> { svc.observe(a) }
        // Idempotent — disposing again is a no-op.
        svc.dispose(listOf(a))
    }

    test("unknown envId throws on any operation") {
        val svc = MultiEnvService(registry())
        val bogus = EnvId("does-not-exist")
        shouldThrow<NoSuchElementException> { svc.observe(bogus) }
        shouldThrow<NoSuchElementException> { svc.step(StepRequest(bogus, 0)) }
        shouldThrow<NoSuchElementException> { svc.fork(bogus) }
    }

    // =========================================================================
    // Stepping
    // =========================================================================

    test("step advances the game using an action ID from the current observation") {
        val svc = MultiEnvService(registry())
        val (envId, opening) = svc.create(twoPlayerConfig())

        val passView = opening.observation.legalActions
            .first { it.kind.contains("Pass", ignoreCase = true) || it.description.contains("Pass", ignoreCase = true) }
        val result = svc.step(StepRequest(envId, passView.actionId))

        // Digest must change, because we advanced.
        result.observation.stateDigest shouldNotBe opening.observation.stateDigest
    }

    test("the returned registry maps IDs to matching engine actions") {
        val svc = MultiEnvService(registry())
        val (envId, opening) = svc.create(twoPlayerConfig())

        val passEntry = opening.registry.legalActions
            .first { (_, la) -> la.action is PassPriority }
        val resolved = opening.registry.resolve(passEntry.first)
        resolved.shouldNotBe(ResolvedAction.Unknown)
        (resolved as ResolvedAction.Legal).action::class shouldBe PassPriority::class
    }

    test("action IDs from a stale observation become invalid after stepping") {
        val svc = MultiEnvService(registry())
        val (envId, opening) = svc.create(twoPlayerConfig())

        // Save an ID from the opening observation.
        val staleId = opening.observation.legalActions.first().actionId

        // Advance — this regenerates the registry.
        svc.step(StepRequest(envId, staleId))

        // The stored `opening.registry` still resolves (client held on to it),
        // but the server's registry for this env has moved on. We exercise the
        // server-side invalidation by asking it to resolve a high action ID
        // that's not present in the current step.
        shouldThrow<IllegalArgumentException> {
            // actionId well outside any realistic legal-actions list size.
            svc.step(StepRequest(envId, actionId = 99_999))
        }
    }

    test("stepBatch advances multiple envs in parallel") {
        val svc = MultiEnvService(registry())
        val envs = (1..4).map { svc.create(twoPlayerConfig()).envId }

        val requests = envs.map { id ->
            val obs = svc.observe(id)
            val actionId = obs.observation.legalActions.first().actionId
            StepRequest(id, actionId)
        }
        val results = svc.stepBatch(requests)

        results shouldHaveSize envs.size
        results.map { it.first } shouldContainAll envs
        results.forEach { (_, res) ->
            res.observation.terminated.shouldBeFalse()
        }
    }

    test("stepBatch with an empty request list returns an empty list") {
        val svc = MultiEnvService(registry())
        svc.stepBatch(emptyList()) shouldBe emptyList()
    }

    // =========================================================================
    // Fork / snapshot / restore
    // =========================================================================

    test("fork creates independent envs that diverge on step") {
        val svc = MultiEnvService(registry())
        val (src, _) = svc.create(twoPlayerConfig())
        val beforeFork = svc.observe(src).observation.stateDigest

        val children = svc.fork(src, count = 2)
        children shouldHaveSize 2
        children.forEach { it shouldNotBe src }

        // Children start with the same digest as their source.
        children.forEach { child ->
            svc.observe(child).observation.stateDigest shouldBe beforeFork
        }

        // Stepping on one child must not affect the source or siblings.
        val actionId = svc.observe(children[0]).observation.legalActions.first().actionId
        svc.step(StepRequest(children[0], actionId))

        svc.observe(src).observation.stateDigest shouldBe beforeFork
        svc.observe(children[1]).observation.stateDigest shouldBe beforeFork
        svc.observe(children[0]).observation.stateDigest shouldNotBe beforeFork
    }

    test("fork with a non-positive count throws") {
        val svc = MultiEnvService(registry())
        val (src, _) = svc.create(twoPlayerConfig())
        shouldThrow<IllegalArgumentException> { svc.fork(src, count = 0) }
    }

    test("snapshot and restore round-trips the game state") {
        val svc = MultiEnvService(registry())
        val (envId, opening) = svc.create(twoPlayerConfig())

        val handle = svc.snapshot(envId)
        val openingDigest = opening.observation.stateDigest

        // Advance a few steps so the restore has something to undo.
        repeat(3) {
            val actionId = svc.observe(envId).observation.legalActions.first().actionId
            svc.step(StepRequest(envId, actionId))
        }
        svc.observe(envId).observation.stateDigest shouldNotBe openingDigest

        val restored = svc.restore(envId, handle)
        restored.observation.stateDigest shouldBe openingDigest
    }

    // =========================================================================
    // Deck resolution
    // =========================================================================

    test("DeckResolver produces a 40+ card sealed deck when a BoosterGenerator is wired up") {
        // Covers the plumbing — that RandomSealed reaches the SealedDeckGenerator
        // and returns a playable-sized list. We don't also spin up a game here
        // because the generator emits variant names (e.g. "Swamp#BLB-270") that
        // require the full app-level basic-land registration in addition to
        // `BloomburrowSet.allCards` — that wiring lives in game-server, not in
        // the gym service.
        val resolver = DeckResolver(registry(), boosterGenerator = boosterGen())
        val deck = resolver.resolve(DeckSpec.RandomSealed(setCode = BloomburrowSet.SET_CODE))
        deck.cards.size shouldBeGreaterThan 39
    }

    test("RandomSealed without a BoosterGenerator fails fast") {
        val svc = MultiEnvService(registry(), boosterGenerator = null)
        val config = EnvConfig(
            players = listOf(
                PlayerSpec("Alice", DeckSpec.RandomSealed()),
                PlayerSpec("Bob", DeckSpec.RandomSealed())
            ),
            skipMulligans = true
        )
        shouldThrow<IllegalArgumentException> { svc.create(config) }
    }

    test("DeckResolver.validate flags unknown cards and too-small decks") {
        val svc = MultiEnvService(registry())
        val resolver = svc.deckResolver

        val ok = resolver.validate(
            mapOf("Mountain" to 30, "Raging Goblin" to 30),
            minSize = 40
        )
        ok.ok.shouldBeTrue()
        ok.totalCards shouldBe 60

        val tooSmall = resolver.validate(mapOf("Mountain" to 5), minSize = 40)
        tooSmall.ok.shouldBeFalse()
        tooSmall.errors.shouldNotBeEmpty()

        val unknown = resolver.validate(
            mapOf("Mountain" to 40, "Nonexistent Card" to 1),
            minSize = 40
        )
        unknown.ok.shouldBeFalse()
        unknown.errors.any { it.contains("Unknown card") }.shouldBeTrue()
    }

    // =========================================================================
    // submitDecision
    // =========================================================================

    test("submitDecision rejects when the env has no pending decision") {
        val svc = MultiEnvService(registry())
        val (envId, _) = svc.create(twoPlayerConfig())

        // Fabricate a response object — content doesn't matter because the
        // guard fails before we look at it.
        val pending = svc.observe(envId).observation.pendingDecision
        pending.shouldBeNull()

        shouldThrow<IllegalStateException> {
            svc.submitDecision(
                envId,
                com.wingedsheep.engine.core.YesNoResponse("no-such-decision", true)
            )
        }
    }

    // =========================================================================
    // Perspective / masking wiring
    // =========================================================================

    test("opening observation uses the configured perspective and reveal flag") {
        val svc = MultiEnvService(registry())
        val created = svc.create(twoPlayerConfig(perspective = 1))

        val me = created.observation.observation.perspectivePlayerId
        created.observation.observation.players
            .first { it.isPerspective }.id shouldBe me

        val opponent = created.observation.observation.players.first { !it.isPerspective }
        // Opponent hand should be hidden by default.
        val opponentHand = created.observation.observation.zones
            .first { it.ownerId == opponent.id && it.zoneType == com.wingedsheep.sdk.core.Zone.HAND }
        opponentHand.hidden.shouldBeTrue()

        // Now observe with revealAll=true — opponent hand becomes visible.
        val revealed = svc.observe(created.envId, revealAll = true).observation
        val openedHand = revealed.zones.first {
            it.ownerId == opponent.id && it.zoneType == com.wingedsheep.sdk.core.Zone.HAND
        }
        openedHand.hidden.shouldBeFalse()
        openedHand.cards.size shouldBe openedHand.size
    }

    // =========================================================================
    // stepCount sanity
    // =========================================================================

    test("stepping the same env multiple times keeps turnNumber monotonically non-decreasing") {
        val svc = MultiEnvService(registry())
        val (envId, _) = svc.create(twoPlayerConfig())

        val turnsSeen = mutableListOf<Int>()
        repeat(8) {
            val obs = svc.observe(envId).observation
            turnsSeen += obs.turnNumber
            if (obs.terminated) return@repeat
            val nextId = obs.legalActions.firstOrNull()?.actionId ?: return@repeat
            svc.step(StepRequest(envId, nextId))
        }

        turnsSeen.zipWithNext { a, b -> (b >= a).shouldBeTrue() }
        turnsSeen.last() shouldBeGreaterThan 0
    }
})
