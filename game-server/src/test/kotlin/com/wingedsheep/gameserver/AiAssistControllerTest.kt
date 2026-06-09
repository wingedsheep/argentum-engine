package com.wingedsheep.gameserver

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.controller.AiAssistController
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.repository.InMemoryLobbyRepository
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * The AI-assist endpoints are stateless w.r.t. the game flow, so they're unit-testable without the
 * full Spring stack: construct the controller directly with a real [CardRegistry] and an in-memory
 * lobby repo. Verifies the per-tournament toggle is enforced server-side.
 */
class AiAssistControllerTest : FunSpec({

    val registry = CardRegistry().apply {
        for (set in MtgSetCatalog.all) {
            register(set.cards)
            register(set.basicLands)
        }
    }
    val pack = registry.allCardNames().take(6).toList()

    fun lobby(assistEnabled: Boolean): TournamentLobby = TournamentLobby(
        setCodes = listOf("POR"),
        setNames = listOf("Portal"),
        boosterGenerator = BoosterGenerator(emptyMap()),
        aiAssistEnabled = assistEnabled,
    )

    test("suggest-pick returns scores when assistance is enabled") {
        val repo = InMemoryLobbyRepository()
        val lobby = lobby(assistEnabled = true)
        repo.saveLobby(lobby)
        val controller = AiAssistController(registry, repo)

        val advice = controller.suggestPick(
            AiAssistController.SuggestPickBody(lobbyId = lobby.lobbyId, pack = pack, picksRequired = 1)
        )

        advice.scores.size shouldBeGreaterThan 0
        advice.recommended.size shouldBe 1
    }

    test("suggest-pick is rejected with 403 when assistance is disabled") {
        val repo = InMemoryLobbyRepository()
        val lobby = lobby(assistEnabled = false)
        repo.saveLobby(lobby)
        val controller = AiAssistController(registry, repo)

        val ex = shouldThrow<ResponseStatusException> {
            controller.suggestPick(
                AiAssistController.SuggestPickBody(lobbyId = lobby.lobbyId, pack = pack)
            )
        }
        ex.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    test("auto-build is rejected with 403 when assistance is disabled") {
        val repo = InMemoryLobbyRepository()
        val lobby = lobby(assistEnabled = false)
        repo.saveLobby(lobby)
        val controller = AiAssistController(registry, repo)

        val ex = shouldThrow<ResponseStatusException> {
            controller.autoBuild(
                AiAssistController.AutoBuildBody(lobbyId = lobby.lobbyId, pool = pack)
            )
        }
        ex.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    test("practice mode (no lobbyId) is allowed even though no lobby grants assistance") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())

        val advice = controller.suggestPick(AiAssistController.SuggestPickBody(lobbyId = null, pack = pack))
        advice.scores.size shouldBeGreaterThan 0

        val deck = controller.autoBuild(AiAssistController.AutoBuildBody(lobbyId = null, pool = pack))
        deck.deckList.values.sum() shouldBeGreaterThan 0
    }

    test("an unknown lobbyId is treated as practice (allowed), not rejected") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())

        // No lobby with this id exists in the repo: requireAssistEnabled returns without throwing.
        val advice = controller.suggestPick(
            AiAssistController.SuggestPickBody(lobbyId = "does-not-exist", pack = pack)
        )
        advice.scores.size shouldBeGreaterThan 0
    }

    test("an unknown advisorId falls back to the default engine instead of throwing") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())

        val advice = controller.suggestPick(
            AiAssistController.SuggestPickBody(advisorId = "no-such-engine", pack = pack)
        )
        // Falls back to the first registered draft advisor (the heuristic).
        advice.advisorId shouldBe "heuristic"
    }

    test("advisorId selects the named engine") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())

        val advice = controller.suggestPick(
            AiAssistController.SuggestPickBody(advisorId = "draftsim", pack = pack)
        )
        advice.advisorId shouldBe "draftsim"
    }

    test("empty pack / empty pool degrade gracefully rather than crashing") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())

        val advice = controller.suggestPick(AiAssistController.SuggestPickBody(pack = emptyList()))
        advice.scores shouldBe emptyList()
        advice.recommended shouldBe emptyList()

        // With no pool there are no spells to choose: the heuristic returns only a basic-land base,
        // never crashing on the empty input.
        val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        val deck = controller.autoBuild(AiAssistController.AutoBuildBody(pool = emptyList()))
        deck.deckList.keys.all { it in basics } shouldBe true
    }

    test("advisor catalog lists the heuristic engines for the dropdowns") {
        val controller = AiAssistController(registry, InMemoryLobbyRepository())
        val advisors = controller.advisors()

        advisors.draft.any { it.id == "heuristic" } shouldBe true
        advisors.deckbuild.any { it.id == "heuristic" } shouldBe true
    }
})
