package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hour of Devastation (HOU #97) — {3}{R}{R} Sorcery.
 * All creatures lose indestructible until end of turn, then 5 damage to each creature and each
 * non-Bolas planeswalker.
 */
class HourOfDevastationScenarioTest : ScenarioTestBase() {

    init {
        context("Hour of Devastation") {

            test("kills indestructible creatures and non-Bolas planeswalkers; Bolas planeswalkers are spared") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hour of Devastation")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withCardOnBattlefield(2, "Ajani, the Greathearted")
                    .withCardOnBattlefield(2, "Nicol Bolas, God-Pharaoh")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hour of Devastation").error shouldBe null
                game.resolveStack()

                withClue("Darksteel Myr lost indestructible and died to 5 damage") {
                    game.isOnBattlefield("Darksteel Myr") shouldBe false
                    game.isInGraveyard(2, "Darksteel Myr") shouldBe true
                }
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("Ajani (5 loyalty) took 5 damage and died") {
                    game.isInGraveyard(2, "Ajani, the Greathearted") shouldBe true
                }
                withClue("Nicol Bolas, God-Pharaoh is a Bolas planeswalker and takes no damage") {
                    val bolas = game.findPermanent("Nicol Bolas, God-Pharaoh")!!
                    game.state.getEntity(bolas)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 7
                }
            }
        }
    }
}
