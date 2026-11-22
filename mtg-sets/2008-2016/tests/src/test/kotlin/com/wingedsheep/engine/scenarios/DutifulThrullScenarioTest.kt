package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Dutiful Thrull (GTC) — {B}: Regenerate this creature. */
class DutifulThrullScenarioTest : ScenarioTestBase() {
    init {
        test("a regeneration shield saves it from lethal damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dutiful Thrull")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val thrull = game.findPermanent("Dutiful Thrull")!!
            val abilityId = cardRegistry.getCard("Dutiful Thrull")!!.script.activatedAbilities.single().id

            val result = game.execute(ActivateAbility(game.player1Id, thrull, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", thrull).error shouldBe null
            game.resolveStack()

            withClue("regenerated instead of dying") {
                game.isOnBattlefield("Dutiful Thrull") shouldBe true
                game.isInGraveyard(1, "Dutiful Thrull") shouldBe false
            }
        }
    }
}
