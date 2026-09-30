package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Viral Spawning (ONE #194) — {2}{G} Sorcery.
 *
 * "Create a 3/3 green Phyrexian Beast creature token with toxic 1.
 *  Corrupted — As long as an opponent has three or more poison counters and this card is in your
 *  graveyard, it has flashback {2}{G}."
 *
 * Proof card for a printed flashback gated by a condition (`KeywordAbility.Flashback.condition`).
 */
class ViralSpawningScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun graveyardGame(opponentPoison: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInGraveyard(1, "Viral Spawning")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        return game
    }

    private fun TestGame.flashbackCast() = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInGraveyard(1, "Viral Spawning").single(),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.FLASHBACK,
        ),
    )

    init {
        test("cast from hand, it creates a 3/3 green Phyrexian Beast with toxic 1 and goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Viral Spawning")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Viral Spawning").error shouldBe null
            game.resolveStack()

            val beast = game.findPermanent("Phyrexian Beast Token") ?: game.findPermanent("Phyrexian Beast")
            withClue("a Beast token was created") { beast shouldNotBe null }
            val projected = game.state.projectedState
            projected.getPower(beast!!) shouldBe 3
            projected.getToughness(beast) shouldBe 3
            projected.getColors(beast) shouldBe setOf(Color.GREEN.name)
            projected.hasSubtype(beast, "Phyrexian") shouldBe true
            projected.hasSubtype(beast, "Beast") shouldBe true
            projected.hasKeyword(beast, Keyword.TOXIC) shouldBe true
            projected.getKeywords(beast).contains("TOXIC_1") shouldBe true
            withClue("a hand cast is not a flashback cast") {
                game.isInGraveyard(1, "Viral Spawning") shouldBe true
            }
        }

        test("with an opponent at three poison counters, flashback {2}{G} casts it from the graveyard and exiles it") {
            val game = graveyardGame(opponentPoison = 3)

            withClue("flashback is offered while corrupted") {
                game.getLegalActions(1).any { it.actionType == "CastWithFlashback" } shouldBe true
            }
            val cast = game.flashbackCast()
            withClue("flashback cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            (game.findPermanent("Phyrexian Beast Token") ?: game.findPermanent("Phyrexian Beast")) shouldNotBe null
            game.isInGraveyard(1, "Viral Spawning") shouldBe false
            game.isInExile(1, "Viral Spawning") shouldBe true
        }

        test("with no opponent at three poison counters, it has no flashback") {
            val game = graveyardGame(opponentPoison = 2)

            withClue("flashback is not offered") {
                game.getLegalActions(1).any { it.actionType == "CastWithFlashback" } shouldBe false
            }
            withClue("and a hand-built flashback cast is rejected") {
                game.flashbackCast().error shouldNotBe null
            }
            game.isInGraveyard(1, "Viral Spawning") shouldBe true
        }

        test("only an opponent's poison counts — the caster's own poison does not corrupt") {
            val game = graveyardGame(opponentPoison = 0)
            game.setPoison(game.player1Id, 5)

            game.getLegalActions(1).any { it.actionType == "CastWithFlashback" } shouldBe false
        }

        test("a spell cast with flashback is exiled when it is countered, not put into the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Viral Spawning")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInHand(2, "Counterspell")
                .withLandsOnBattlefield(2, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.setPoison(game.player2Id, 3)

            game.flashbackCast().error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Counterspell", "Viral Spawning").error shouldBe null
            game.resolveStack()

            game.findPermanent("Phyrexian Beast Token") shouldBe null
            game.findPermanent("Phyrexian Beast") shouldBe null
            withClue("CR 702.34a: exiled any time it would leave the stack") {
                game.isInGraveyard(1, "Viral Spawning") shouldBe false
                game.isInExile(1, "Viral Spawning") shouldBe true
            }
        }

        test("a spell cast with flashback is still exiled if corrupted lapses before it resolves") {
            val game = graveyardGame(opponentPoison = 3)

            game.flashbackCast().error shouldBe null
            game.setPoison(game.player2Id, 0)
            game.resolveStack()

            (game.findPermanent("Phyrexian Beast Token") ?: game.findPermanent("Phyrexian Beast")) shouldNotBe null
            withClue("flashback's exile follows the cast, not the condition") {
                game.isInGraveyard(1, "Viral Spawning") shouldBe false
                game.isInExile(1, "Viral Spawning") shouldBe true
            }
        }
    }
}
