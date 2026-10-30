package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.atq.cards.Ornithopter
import com.wingedsheep.mtg.sets.definitions.kld.cards.PadeemConsulOfInnovation
import com.wingedsheep.mtg.sets.definitions.m20.cards.ManifoldKey
import com.wingedsheep.mtg.sets.definitions.mbs.cards.ShimmerMyr
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Padeem, Consul of Innovation (KLD #59).
 *
 * "At the beginning of your upkeep, if you control the artifact with the greatest mana value or
 * tied for the greatest mana value, draw a card." The intervening-if compares the greatest mana
 * value among your artifacts with the greatest among all artifacts, and requires you to control an
 * artifact at all — so an empty artifact board doesn't "tie" an opposing 0-mana-value artifact.
 */
class PadeemConsulOfInnovationScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(PadeemConsulOfInnovation, ManifoldKey, ShimmerMyr, Ornithopter))

        /** Opponent's main phase; passing reaches Player1's upkeep, where Padeem's trigger is checked. */
        fun board(yourArtifacts: List<String>, theirArtifacts: List<String>) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Padeem, Consul of Innovation")
            .apply { yourArtifacts.forEach { withCardOnBattlefield(1, it) } }
            .apply { theirArtifacts.forEach { withCardOnBattlefield(2, it) } }
            .apply { repeat(5) { withCardInLibrary(1, "Island") } }
            .apply { repeat(5) { withCardInLibrary(2, "Island") } }
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun drawsAtUpkeep(yourArtifacts: List<String>, theirArtifacts: List<String>): Boolean {
            val game = board(yourArtifacts, theirArtifacts)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val before = game.handSize(1)
            game.resolveStack()
            return game.handSize(1) == before + 1
        }

        test("you control the artifact with the greatest mana value: draw") {
            drawsAtUpkeep(listOf("Shimmer Myr"), listOf("Manifold Key")) shouldBe true
        }

        test("tied for the greatest mana value: draw") {
            drawsAtUpkeep(listOf("Manifold Key"), listOf("Manifold Key")) shouldBe true
        }

        test("an opponent's artifact has the greater mana value: no draw") {
            drawsAtUpkeep(listOf("Manifold Key"), listOf("Shimmer Myr")) shouldBe false
        }

        test("no artifacts of your own never ties an opponent's 0-mana-value artifact") {
            withClue("Padeem itself is not an artifact") {
                drawsAtUpkeep(emptyList(), listOf("Ornithopter")) shouldBe false
            }
        }
    }
}
