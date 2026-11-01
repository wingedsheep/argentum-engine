package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Conclave Guildmage (GRN #162) — {G}{W} 2/2 Elf Cleric.
 *
 *   {G}, {T}: Creatures you control gain trample until end of turn.
 *   {5}{W}, {T}: Create a 2/2 green and white Elf Knight creature token with vigilance.
 */
class ConclaveGuildmageScenarioTest : ScenarioTestBase() {
    private fun TestGame.activate(index: Int) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Conclave Guildmage")!!,
                abilityId = cardRegistry.getCard("Conclave Guildmage")!!.script.activatedAbilities[index].id
            )
        )

    init {
        test("{G}, {T}: creatures you control gain trample, opponent's creatures don't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Conclave Guildmage")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(0).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent("Conclave Guildmage")!!, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.TRAMPLE) shouldBe false
        }

        test("{5}{W}, {T}: create a 2/2 Elf Knight token with vigilance") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Conclave Guildmage")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(1).error shouldBe null
            game.resolveStack()

            val knight = game.findPermanents("Elf Knight Token").single()
            game.state.projectedState.getPower(knight) shouldBe 2
            game.state.projectedState.getToughness(knight) shouldBe 2
            game.state.projectedState.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
        }
    }
}
