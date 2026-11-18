package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m10.cards.CudgelTroll
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reprisal (ALL #13a) — {1}{W} Instant.
 *
 *   Destroy target creature with power 4 or greater. It can't be regenerated.
 */
class ReprisalScenarioTest : ScenarioTestBase() {

    private val regenerateId = CudgelTroll.activatedAbilities.first().id

    init {
        test("destroys a power-4 creature even through a regeneration shield") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Reprisal")
                .withCardOnBattlefield(1, "Cudgel Troll")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val troll = game.findPermanent("Cudgel Troll")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = troll, abilityId = regenerateId)
            ).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Reprisal", troll).error shouldBe null
            game.resolveStack()

            withClue("the regeneration shield can't save it") {
                game.isInGraveyard(1, "Cudgel Troll") shouldBe true
            }
        }

        test("a creature with power 3 or less isn't a legal target") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Reprisal")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Reprisal", giant).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
