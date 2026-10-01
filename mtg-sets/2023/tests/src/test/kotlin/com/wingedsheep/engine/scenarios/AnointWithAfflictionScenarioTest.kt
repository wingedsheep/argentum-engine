package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Anoint with Affliction (ONE #81) — {1}{B} Instant.
 *
 * "Exile target creature if it has mana value 3 or less.
 *  Corrupted — Exile that creature instead if its controller has three or more poison counters."
 */
class AnointWithAfflictionScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(creature: String, opponentPoison: Int = 0, casterPoison: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Anoint with Affliction")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(2, creature)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (opponentPoison > 0) game.setPoison(game.player2Id, opponentPoison)
        if (casterPoison > 0) game.setPoison(game.player1Id, casterPoison)
        return game
    }

    private fun TestGame.castAt(creature: String) {
        val target = findPermanent(creature)!!
        castSpell(1, "Anoint with Affliction", target).error shouldBe null
        resolveStack()
    }

    init {
        test("exiles a creature with mana value 3 or less") {
            val game = game("Grizzly Bears")
            game.castAt("Grizzly Bears")
            withClue("Grizzly Bears (MV 2) should be exiled") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }

        test("a creature with mana value 4 or more is untouched without corrupted") {
            val game = game("Craw Wurm", opponentPoison = 2)
            game.castAt("Craw Wurm")
            withClue("Craw Wurm (MV 6) stays when its controller has only two poison") {
                game.isOnBattlefield("Craw Wurm") shouldBe true
                game.isInExile(2, "Craw Wurm") shouldBe false
            }
            game.isInGraveyard(1, "Anoint with Affliction") shouldBe true
        }

        test("corrupted — exiles a big creature when its controller has three poison") {
            val game = game("Craw Wurm", opponentPoison = 3)
            game.castAt("Craw Wurm")
            withClue("Craw Wurm should be exiled under corrupted") {
                game.isInExile(2, "Craw Wurm") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe false
            }
        }

        test("the caster's own poison doesn't count — only the target's controller's") {
            val game = game("Craw Wurm", casterPoison = 5)
            game.castAt("Craw Wurm")
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
