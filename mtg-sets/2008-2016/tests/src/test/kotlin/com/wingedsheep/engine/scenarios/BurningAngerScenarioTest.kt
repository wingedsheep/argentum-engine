package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.BurningAnger
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe

/**
 * Burning Anger: the enchanted creature has "{T}: This creature deals damage equal to its power to
 * any target." The power is the enchanted creature's, read when the ability resolves.
 */
class BurningAngerScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = (BurningAnger.staticAbilities.single() as GrantActivatedAbility).ability.id

        test("the enchanted creature taps to deal damage equal to its power to a player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Burning Anger", "Hill Giant")
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = giant, abilityId = grantedId,
                    targets = listOf(ChosenTarget.Player(game.player2Id))
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
        }

        test("it can target a creature too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Burning Anger", "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .build()
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = game.findPermanent("Grizzly Bears")!!, abilityId = grantedId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Savannah Lions")!!))
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Savannah Lions") shouldBe true
        }
    }
}
