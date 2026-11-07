package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m12.cards.GoblinFireslinger
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Goblin Fireslinger (M12 #139) — {R} 1/1.
 * "{T}: This creature deals 1 damage to target player or planeswalker."
 */
class GoblinFireslingerScenarioTest : ScenarioTestBase() {
    init {
        test("tapping it deals 1 damage to the target player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Goblin Fireslinger")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val slinger = game.findPermanent("Goblin Fireslinger")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = slinger,
                    abilityId = GoblinFireslinger.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
            game.state.getEntity(slinger)?.has<TappedComponent>() shouldBe true
        }
    }
}
