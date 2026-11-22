package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Sunhome Guildmage (GTC) — {1}{R}{W}: team +1/+0; {2}{R}{W}: 1/1 red and white Soldier with haste. */
class SunhomeGuildmageScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Sunhome Guildmage")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("first ability gives creatures you control +1/+0") {
            val game = board()
            val guildmage = game.findPermanent("Sunhome Guildmage")!!
            val abilityId = cardRegistry.getCard("Sunhome Guildmage")!!.script.activatedAbilities[0].id

            val result = game.execute(ActivateAbility(game.player1Id, guildmage, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanents("Grizzly Bears")
            val mine = bears.single { projected.getController(it) == game.player1Id }
            val theirs = bears.single { projected.getController(it) == game.player2Id }
            projected.getPower(guildmage) shouldBe 3
            projected.getToughness(guildmage) shouldBe 2
            projected.getPower(mine) shouldBe 3
            projected.getPower(theirs) shouldBe 2
        }

        test("second ability creates a 1/1 red and white Soldier token with haste") {
            val game = board()
            val guildmage = game.findPermanent("Sunhome Guildmage")!!
            val abilityId = cardRegistry.getCard("Sunhome Guildmage")!!.script.activatedAbilities[1].id

            val result = game.execute(ActivateAbility(game.player1Id, guildmage, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val tokens = game.findPermanents("Soldier Token")
            tokens shouldHaveSize 1
            val token = tokens.single()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasKeyword(token, Keyword.HASTE) shouldBe true
            projected.getColors(token) shouldBe setOf(Color.RED.name, Color.WHITE.name)
        }
    }
}
