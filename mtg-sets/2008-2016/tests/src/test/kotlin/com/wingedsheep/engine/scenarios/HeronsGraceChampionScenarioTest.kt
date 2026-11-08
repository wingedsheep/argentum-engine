package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Heron's Grace Champion (EMN #185) — {2}{G}{W} Creature — Human Knight 3/3.
 *
 *   Flash
 *   Lifelink
 *   When this creature enters, other Humans you control get +1/+1 and gain lifelink until end of turn.
 *
 * The enter trigger touches only *other* Humans *you* control: not the Champion itself, not a
 * non-Human of yours, not an opponent's Human.
 */
class HeronsGraceChampionScenarioTest : ScenarioTestBase() {

    init {
        test("entering pumps and grants lifelink to your other Humans only, until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Heron's Grace Champion")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Elite Vanguard") // Human Soldier 2/1
                .withCardOnBattlefield(1, "Grizzly Bears") // Bear 2/2
                .withCardOnBattlefield(2, "Elite Vanguard") // opponent's Human
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (myVanguard, theirVanguard) = game.findAllPermanents("Elite Vanguard")
                .partition { game.state.getBattlefield(game.player1Id).contains(it) }
                .let { (mine, theirs) -> mine.single() to theirs.single() }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Heron's Grace Champion").error shouldBe null
            game.resolveStack()

            val champion = game.findPermanent("Heron's Grace Champion")!!
            val projected = game.state.projectedState

            withClue("your other Human gets +1/+1 and lifelink") {
                projected.getPower(myVanguard) shouldBe 3
                projected.getToughness(myVanguard) shouldBe 2
                projected.hasKeyword(myVanguard, Keyword.LIFELINK) shouldBe true
            }
            withClue("the Champion is excluded from its own pump") {
                projected.getPower(champion) shouldBe 3
                projected.getToughness(champion) shouldBe 3
                projected.hasKeyword(champion, Keyword.LIFELINK) shouldBe true
                projected.hasKeyword(champion, Keyword.FLASH) shouldBe true
            }
            withClue("a non-Human you control is untouched") {
                projected.getPower(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
            }
            withClue("an opponent's Human is untouched") {
                projected.getPower(theirVanguard) shouldBe 2
                projected.hasKeyword(theirVanguard, Keyword.LIFELINK) shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the pump ends at end of turn") {
                game.state.projectedState.getPower(myVanguard) shouldBe 2
                game.state.projectedState.hasKeyword(myVanguard, Keyword.LIFELINK) shouldBe false
            }
        }
    }
}
