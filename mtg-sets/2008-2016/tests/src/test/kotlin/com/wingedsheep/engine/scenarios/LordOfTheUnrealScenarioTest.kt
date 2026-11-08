package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lord of the Unreal (M12 #62) — {U}{U} 2/2.
 * "Illusion creatures you control get +1/+1 and have hexproof."
 */
class LordOfTheUnrealScenarioTest : ScenarioTestBase() {
    init {
        test("your Illusions get +1/+1 and hexproof; an opponent's Illusion and the Lord itself don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Lord of the Unreal")
                .withCardOnBattlefield(1, "Phantom Monster")
                .withCardOnBattlefield(2, "Palinchron")
                .withCardInHand(2, "Shock")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val lord = game.findPermanent("Lord of the Unreal")!!
            val monster = game.findPermanent("Phantom Monster")!!
            val palinchron = game.findPermanent("Palinchron")!!
            val projected = game.state.projectedState

            projected.getPower(monster) shouldBe 4
            projected.getToughness(monster) shouldBe 4
            projected.hasKeyword(monster, Keyword.HEXPROOF) shouldBe true
            withClue("the opponent's Illusion isn't affected") {
                projected.getPower(palinchron) shouldBe 4
                projected.hasKeyword(palinchron, Keyword.HEXPROOF) shouldBe false
            }
            withClue("the Lord is a Human Wizard, not an Illusion") {
                projected.getPower(lord) shouldBe 2
                projected.hasKeyword(lord, Keyword.HEXPROOF) shouldBe false
            }

            withClue("the opponent can't target the hexproof Illusion") {
                game.castSpell(2, "Shock", monster).error shouldNotBe null
            }
            game.castSpell(2, "Shock", lord).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Lord of the Unreal") shouldBe true
        }
    }
}
