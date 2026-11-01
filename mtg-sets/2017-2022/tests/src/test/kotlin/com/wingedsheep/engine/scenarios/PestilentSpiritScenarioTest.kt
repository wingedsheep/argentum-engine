package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pestilent Spirit (RNA #81) — {2}{B} 3/2 Spirit. "Menace, deathtouch. Instant and sorcery spells
 * you control have deathtouch."
 *
 * First card to grant deathtouch to spells through the static [GrantKeywordToOwnSpells] channel
 * (Judith grants it one-shot via the spell-granted-keywords component). A spell on the stack is not
 * projected, so the kill depends on the noncombat damage path reading the static grant live. A 3/3
 * surviving 2 damage unless deathtouch applied makes the kill the assertion; the opponent's Shock
 * is the control for "spells *you* control".
 */
class PestilentSpiritScenarioTest : ScenarioTestBase() {

    init {
        context("Pestilent Spirit") {

            test("your Shock has deathtouch, so 2 damage kills a 3/3") {
                val game = scenario()
                    .withPlayers("Spirit", "Opponent")
                    .withCardOnBattlefield(1, "Pestilent Spirit")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castSpell(1, "Shock", courser)
                withClue("casting Shock should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("a 3/3 dealt 2 deathtouch damage is destroyed") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
            }

            test("an opponent's Shock does not gain deathtouch") {
                val game = scenario()
                    .withPlayers("Spirit", "Opponent")
                    .withCardOnBattlefield(1, "Pestilent Spirit")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val cast = game.castSpell(2, "Shock", courser)
                withClue("casting Shock should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("the grant covers only spells Pestilent Spirit's controller controls") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                }
            }
        }
    }
}
