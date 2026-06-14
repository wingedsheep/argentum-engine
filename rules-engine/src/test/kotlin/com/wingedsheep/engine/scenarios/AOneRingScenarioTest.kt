package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A-The One Ring — the Alchemy rebalance of The One Ring. Identical except the draw ability
 * costs {1}, {T} instead of just {T}. This test pins the extra mana cost: with an untapped
 * land the ability resolves (burden + draw); the Gap 8 protection path is covered by
 * [TheOneRingScenarioTest].
 */
class AOneRingScenarioTest : ScenarioTestBase() {

    private val tapAbilityId by lazy {
        cardRegistry.requireCard("A-The One Ring").activatedAbilities[0].id
    }

    init {
        test("{1},{T} adds a burden counter and draws that many") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "A-The One Ring")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ring = game.findPermanent("A-The One Ring")!!
            val handBefore = game.handSize(1)

            // The {1} is auto-paid from the untapped Island.
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = ring, abilityId = tapAbilityId)
            ).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("{1},{T} can't be activated without the mana available") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "A-The One Ring")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ring = game.findPermanent("A-The One Ring")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = ring, abilityId = tapAbilityId)
            ).error shouldNotBe null
        }
    }
}
