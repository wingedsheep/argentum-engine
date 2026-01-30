package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Tidal Surge.
 *
 * Card reference:
 * - Tidal Surge (1U): Sorcery. "Tap up to three target creatures without flying."
 *
 * This tests that:
 * - Tidal Surge can target and tap creatures without flying
 * - Multiple creatures can be targeted (up to 3)
 * - Creatures with flying cannot be targeted
 * - 0 targets is valid (optional targeting)
 */
class TidalSurgeScenarioTest : ScenarioTestBase() {

    init {
        context("Tidal Surge taps creatures without flying") {
            test("Tidal Surge taps a single creature without flying") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)  // Enough mana for {1}{U}
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2, no flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Verify bears start untapped
                withClue("Grizzly Bears should start untapped") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe false
                }

                // Cast Tidal Surge targeting the bears
                val castResult = castSpellTargetingPermanents(game, 1, "Tidal Surge", listOf(bearsId))
                withClue("Tidal Surge should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify bears are now tapped
                withClue("Grizzly Bears should be tapped after Tidal Surge resolves") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                }

                // Verify Tidal Surge is in the graveyard
                withClue("Tidal Surge should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Tidal Surge") shouldBe true
                }
            }

            test("Tidal Surge can tap up to three creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")      // Creature 1
                    .withCardOnBattlefield(2, "Hill Giant")         // Creature 2
                    .withCardOnBattlefield(2, "Elvish Ranger")     // Creature 3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val giantId = game.findPermanent("Hill Giant")!!
                val rangersId = game.findPermanent("Elvish Ranger")!!

                // Verify all creatures start untapped
                game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe false
                game.state.getEntity(giantId)?.has<TappedComponent>() shouldBe false
                game.state.getEntity(rangersId)?.has<TappedComponent>() shouldBe false

                // Cast Tidal Surge targeting all three creatures
                val castResult = castSpellTargetingPermanents(
                    game, 1, "Tidal Surge",
                    listOf(bearsId, giantId, rangersId)
                )
                withClue("Tidal Surge should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify all three creatures are now tapped
                withClue("Grizzly Bears should be tapped") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                }
                withClue("Hill Giant should be tapped") {
                    game.state.getEntity(giantId)?.has<TappedComponent>() shouldBe true
                }
                withClue("Elvish Ranger should be tapped") {
                    game.state.getEntity(rangersId)?.has<TappedComponent>() shouldBe true
                }
            }

            test("Tidal Surge can tap fewer than three creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Cast Tidal Surge targeting only two creatures
                val castResult = castSpellTargetingPermanents(
                    game, 1, "Tidal Surge",
                    listOf(bearsId, giantId)
                )
                withClue("Tidal Surge should be cast successfully targeting 2 creatures") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Both creatures should be tapped
                game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(giantId)?.has<TappedComponent>() shouldBe true
            }

            test("Tidal Surge can be cast with zero targets") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Tidal Surge with no targets (valid for "up to" targeting)
                val castResult = castSpellTargetingPermanents(game, 1, "Tidal Surge", emptyList())
                withClue("Tidal Surge should be cast successfully with 0 targets: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Spell should resolve (and do nothing)
                game.isInGraveyard(1, "Tidal Surge") shouldBe true
            }

            test("Tidal Surge skips already tapped creatures") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)  // Already tapped
                    .withCardOnBattlefield(2, "Hill Giant")  // Untapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Cast Tidal Surge targeting both
                val castResult = castSpellTargetingPermanents(
                    game, 1, "Tidal Surge",
                    listOf(bearsId, giantId)
                )
                castResult.error shouldBe null

                game.resolveStack()

                // Both should be tapped (bears was already tapped, giant gets tapped)
                game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(giantId)?.has<TappedComponent>() shouldBe true
            }

            test("Tidal Surge cannot target creatures with flying") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Arrogant Vampire")  // Has flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vampireId = game.findPermanent("Arrogant Vampire")!!

                // Attempting to cast Tidal Surge targeting a creature with flying should fail
                val castResult = castSpellTargetingPermanents(game, 1, "Tidal Surge", listOf(vampireId))
                withClue("Tidal Surge should fail to target a creature with flying") {
                    castResult.error shouldNotBe null
                }

                // Vampire should remain untapped
                game.state.getEntity(vampireId)?.has<TappedComponent>() shouldBe false
            }

            test("Tidal Surge only targets creatures without flying when mixed creatures present") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Tidal Surge")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")      // No flying
                    .withCardOnBattlefield(2, "Arrogant Vampire")   // Has flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val vampireId = game.findPermanent("Arrogant Vampire")!!

                // Cast Tidal Surge targeting only the bears (valid target)
                val castResult = castSpellTargetingPermanents(game, 1, "Tidal Surge", listOf(bearsId))
                withClue("Tidal Surge should succeed targeting creature without flying") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Bears should be tapped, vampire should remain untapped
                game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(vampireId)?.has<TappedComponent>() shouldBe false
            }
        }
    }

    /**
     * Helper to cast a spell targeting multiple permanents.
     */
    private fun castSpellTargetingPermanents(
        game: TestGame,
        playerNumber: Int,
        spellName: String,
        targetIds: List<com.wingedsheep.sdk.model.EntityId>
    ): com.wingedsheep.engine.core.ExecutionResult {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        val hand = game.state.getHand(playerId)
        val cardId = hand.find { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == spellName
        } ?: error("Card '$spellName' not found in player $playerNumber's hand")

        val targets = targetIds.map { ChosenTarget.Permanent(it) }
        return game.execute(CastSpell(playerId, cardId, targets))
    }
}
