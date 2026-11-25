package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crimson Muckwader (M13 #127) — {1}{R} 2/1 Creature — Lizard.
 *   This creature gets +1/+1 as long as you control a Swamp.
 *   {2}{B}: Regenerate this creature.
 */
class CrimsonMuckwaderScenarioTest : ScenarioTestBase() {
    init {
        val regenId = cardRegistry.getCard("Crimson Muckwader")!!.script.activatedAbilities.single().id

        test("is 2/1 without a Swamp") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Crimson Muckwader")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val muck = game.findPermanent("Crimson Muckwader")!!
            game.state.projectedState.getPower(muck) shouldBe 2
            game.state.projectedState.getToughness(muck) shouldBe 1
        }

        test("a Swamp makes it 3/2, and {2}{B} regenerates it through Murder") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Crimson Muckwader")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val muck = game.findPermanent("Crimson Muckwader")!!
            game.state.projectedState.getPower(muck) shouldBe 3
            game.state.projectedState.getToughness(muck) shouldBe 2

            val regen = game.execute(ActivateAbility(game.player1Id, muck, regenId))
            withClue("regenerate should activate: ${regen.error}") { regen.error shouldBe null }
            game.resolveStack()

            val murder = game.castSpell(1, "Murder", muck)
            withClue("Murder should cast: ${murder.error}") { murder.error shouldBe null }
            game.resolveStack()

            withClue("the shield replaces the destruction") { game.isOnBattlefield("Crimson Muckwader") shouldBe true }
            game.state.getEntity(muck)?.has<TappedComponent>() shouldBe true
        }
    }
}
