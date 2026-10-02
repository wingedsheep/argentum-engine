package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zealot's Conviction (ONE #39) — {W} Enchantment — Aura.
 *
 * "Flash / Enchant creature / Enchanted creature gets +1/+1.
 *  Corrupted — As long as an opponent has three or more poison counters, enchanted creature gets an
 *  additional +1/+0 and has first strike."
 */
class ZealotsConvictionScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    /** Casts the Aura at instant speed (beginning of combat) onto Grizzly Bears. */
    private fun enchantBears(): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "Zealot's Conviction")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!
        withClue("flash lets the Aura be cast outside a main phase") {
            game.castSpell(1, "Zealot's Conviction", bears).error shouldBe null
        }
        game.resolveStack()
        withClue("the Aura resolved onto the battlefield") {
            (game.findPermanent("Zealot's Conviction") != null) shouldBe true
        }
        return game to bears
    }

    init {
        test("without corrupted, enchanted creature gets only +1/+1 and no first strike") {
            val (game, bears) = enchantBears()
            game.setPoison(game.player2Id, 2)
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
        }

        test("with an opponent at three poison, enchanted creature gets +2/+1 and first strike") {
            val (game, bears) = enchantBears()
            game.setPoison(game.player2Id, 3)
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("poison on yourself does not turn on corrupted") {
            val (game, bears) = enchantBears()
            game.setPoison(game.player1Id, 5)
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
        }
    }
}
