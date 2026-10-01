package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hexgold Slash (ONE #137) — {R} Instant.
 *
 * "Hexgold Slash deals 2 damage to target creature. If that creature has toxic, Hexgold Slash
 * deals 4 damage to that creature instead."
 */
class HexgoldSlashScenarioTest : ScenarioTestBase() {

    init {
        test("a creature without toxic is dealt 2 damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hexgold Slash")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Hexgold Slash", giant).error shouldBe null
            game.resolveStack()

            withClue("a 3/3 without toxic survives with exactly 2 damage marked") {
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            }
        }

        test("a creature with toxic is dealt 4 damage instead") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hexgold Slash")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Ichorspit Basilisk")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val basilisk = game.findPermanent("Ichorspit Basilisk")!!
            game.castSpell(1, "Hexgold Slash", basilisk).error shouldBe null
            game.resolveStack()

            withClue("Ichorspit Basilisk is a 1/3 with toxic 1 — 4 damage kills it") {
                game.isOnBattlefield("Ichorspit Basilisk") shouldBe false
                game.isInGraveyard(2, "Ichorspit Basilisk") shouldBe true
            }
        }
    }
}
