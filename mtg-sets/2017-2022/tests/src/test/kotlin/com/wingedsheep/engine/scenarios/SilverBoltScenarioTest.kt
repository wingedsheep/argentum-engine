package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Silver Bolt (MID #258) — "{3}, {T}, Sacrifice this artifact: It deals 3 damage to target
 * creature. If a Werewolf is dealt damage this way, destroy it."
 */
class SilverBoltScenarioTest : ScenarioTestBase() {

    private fun TestGame.bolt(targetName: String) {
        val target = findPermanent(targetName)!!
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Silver Bolt")!!,
                abilityId = cardRegistry.getCard("Silver Bolt")!!.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(target)),
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun board(creature: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Silver Bolt")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(2, creature)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Silver Bolt") {

            test("a Werewolf the damage can't kill is destroyed") {
                val game = board("Lesser Werewolf") // 2/4 Werewolf
                game.bolt("Lesser Werewolf")

                game.isInGraveyard(1, "Silver Bolt") shouldBe true
                game.isInGraveyard(2, "Lesser Werewolf") shouldBe true
            }

            test("a non-Werewolf only takes the 3 damage") {
                val game = board("Force of Nature") // 5/5
                game.bolt("Force of Nature")

                game.isInGraveyard(1, "Silver Bolt") shouldBe true
                game.isOnBattlefield("Force of Nature") shouldBe true
            }
        }
    }
}
