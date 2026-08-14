package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dauntless Bodyguard (DOM #14).
 *
 * As Dauntless Bodyguard enters the battlefield, choose another creature you control.
 */
class DauntlessBodyguardScenarioTest : ScenarioTestBase() {

    private val animateAbilityId by lazy {
        cardRegistry.requireCard("Tough Cookie").activatedAbilities[0].id
    }

    init {
        test("an animated noncreature permanent can be chosen as Bodyguard enters") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Tough Cookie", summoningSickness = false)
                .withCardOnBattlefield(1, "Food")
                .withCardInHand(1, "Dauntless Bodyguard")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cookie = game.findPermanent("Tough Cookie")!!
            val food = game.findPermanent("Food")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = cookie,
                    abilityId = animateAbilityId,
                    targets = listOf(ChosenTarget.Permanent(food))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.projectedState.isCreature(food) shouldBe true

            game.castSpell(1, "Dauntless Bodyguard").error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            choice.options shouldContain food
        }
    }
}
