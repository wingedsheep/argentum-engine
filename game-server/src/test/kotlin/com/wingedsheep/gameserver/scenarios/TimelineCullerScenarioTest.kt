package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Timeline Culler.
 *
 * Card reference:
 * - Timeline Culler ({B}{B}): 2/2 Creature — Drix Warlock
 *   "Haste
 *    You may cast this card from your graveyard using its warp ability.
 *    Warp—{B}, Pay 2 life."
 *
 * Exercises two new wrinkles on the warp mechanic that Timeline Culler is the
 * first card to need:
 *   1. Warp with a non-mana additional cost (Pay 2 life) — the additional cost
 *      bundled with the Warp keyword ability is auto-paid on the warp cast path.
 *   2. Warp from the graveyard — opt-in via the new `fromGraveyard` flag, which
 *      relaxes the default CR 702.185a hand-only restriction.
 */
class TimelineCullerScenarioTest : ScenarioTestBase() {

    init {
        context("Timeline Culler — warp with additional cost and graveyard access") {

            test("warp from hand pays {B} and 2 life and applies WarpedComponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Timeline Culler")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Timeline Culler"
                }

                val result = game.execute(
                    CastSpell(game.player1Id, cardId, useAlternativeCost = true)
                )
                withClue("Warp cast from hand should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Timeline Culler should be on the battlefield") {
                    game.isOnBattlefield("Timeline Culler") shouldBe true
                }
                val permanentId = game.findPermanent("Timeline Culler")!!
                withClue("Permanent should be marked as warped") {
                    game.state.getEntity(permanentId)?.has<WarpedComponent>() shouldBe true
                }
                withClue("Player 1 should have paid 2 life (20 → 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("spellWarpedThisTurn should be set") {
                    game.state.spellWarpedThisTurn shouldBe true
                }
            }

            test("warp from graveyard pays {B} and 2 life and works at sorcery speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Timeline Culler")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Timeline Culler"
                }

                val result = game.execute(
                    CastSpell(game.player1Id, cardId, useAlternativeCost = true)
                )
                withClue("Warp cast from graveyard should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Timeline Culler should be on the battlefield") {
                    game.isOnBattlefield("Timeline Culler") shouldBe true
                }
                val permanentId = game.findPermanent("Timeline Culler")!!
                withClue("Permanent should be marked as warped") {
                    game.state.getEntity(permanentId)?.has<WarpedComponent>() shouldBe true
                }
                withClue("Player 1 should have paid 2 life (20 → 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("hard cast from hand for {B}{B} does not pay life and does not warp") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Timeline Culler")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Timeline Culler")
                withClue("Hard cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val permanentId = game.findPermanent("Timeline Culler")!!
                withClue("Hard cast does not get WarpedComponent") {
                    game.state.getEntity(permanentId)?.has<WarpedComponent>() shouldBe false
                }
                withClue("Player 1 life total unchanged on hard cast") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("spellWarpedThisTurn should remain false") {
                    game.state.spellWarpedThisTurn shouldBe false
                }
            }

            test("warp is rejected when player can't afford the life payment") {
                // CR 119.4 — a player can't pay life unless their life total is at
                // least the amount of the payment. Timeline Culler's warp costs 2 life,
                // so a player at 1 life must not be allowed to warp it.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Timeline Culler")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Timeline Culler"
                }

                val result = game.execute(
                    CastSpell(game.player1Id, cardId, useAlternativeCost = true)
                )
                withClue("Warp should be rejected at 1 life (needs 2): ${result.error}") {
                    result.error shouldNotBe null
                }
                withClue("Life total must remain unchanged on rejected cast") {
                    game.getLifeTotal(1) shouldBe 1
                }
            }

            test("warp from graveyard requires the graveyard opt-in (regression guard)") {
                // Sinister Cryologist's warp has fromGraveyard = false (default).
                // It must NOT be castable from the graveyard via warp.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Sinister Cryologist")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getGraveyard(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Sinister Cryologist"
                }

                val result = game.execute(
                    CastSpell(game.player1Id, cardId, useAlternativeCost = true)
                )
                withClue("Default warp must remain hand-only — cast from graveyard should fail") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
