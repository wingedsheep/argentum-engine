package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Legion Guildmage (GRN #187) — {R}{W} 2/2 Human Wizard.
 *
 *   {5}{R}, {T}: This creature deals 3 damage to each opponent.
 *   {2}{W}, {T}: Tap another target creature.
 */
class LegionGuildmageScenarioTest : ScenarioTestBase() {
    private fun TestGame.activate(index: Int, targets: List<ChosenTarget> = emptyList()) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Legion Guildmage")!!,
                abilityId = cardRegistry.getCard("Legion Guildmage")!!.script.activatedAbilities[index].id,
                targets = targets
            )
        )

    init {
        test("{5}{R}, {T}: deals 3 damage to each opponent, not to you") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Legion Guildmage")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(0).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }

        test("{2}{W}, {T}: taps another target creature, but can't target itself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Legion Guildmage")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val guildmage = game.findPermanent("Legion Guildmage")!!
            game.activate(1, listOf(ChosenTarget.Permanent(guildmage))).error shouldNotBe null

            val giant = game.findPermanent("Hill Giant")!!
            game.activate(1, listOf(ChosenTarget.Permanent(giant))).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
        }
    }
}
