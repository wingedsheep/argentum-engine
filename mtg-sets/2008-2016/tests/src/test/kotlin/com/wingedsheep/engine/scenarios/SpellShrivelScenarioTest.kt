package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Spell Shrivel — counters a spell whose controller can't pay {4}, exiling it instead of putting it
 * into the graveyard.
 */
class SpellShrivelScenarioTest : ScenarioTestBase() {
    init {
        test("an unpaid spell is countered and exiled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Spell Shrivel")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Spell Shrivel", "Grizzly Bears").error shouldBe null
            game.resolveStack()
            if (game.getPendingDecision() is YesNoDecision) {
                game.answerYesNo(false)
                game.resolveStack()
            }

            withClue("countered this way, it goes to exile, not the graveyard") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
