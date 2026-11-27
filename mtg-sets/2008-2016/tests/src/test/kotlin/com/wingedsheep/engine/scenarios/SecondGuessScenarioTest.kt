package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Second Guess (AVR #74) — "Counter target spell that's the second spell cast this turn."
 *
 * The turn's cast order counts every player's spells, and a spell keeps its position after the
 * spells before it have resolved; Second Guess itself is the third (or later) spell, never a target.
 */
class SecondGuessScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Shock")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInHand(2, "Lightning Bolt")
        .withCardInHand(2, "Second Guess")
        .withLandsOnBattlefield(2, "Mountain", 1)
        .withLandsOnBattlefield(2, "Island", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("counters the second spell even when an opponent cast it; the first spell isn't a legal target") {
            val game = board()
            val life1 = game.getLifeTotal(1)
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.passPriority()
            // Player 2 responds: their Lightning Bolt is the turn's second spell.
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            withClue("Shock was the first spell cast this turn") {
                game.castSpellTargetingStackSpell(2, "Second Guess", "Shock").error shouldNotBe null
            }
            game.castSpellTargetingStackSpell(2, "Second Guess", "Lightning Bolt").error shouldBe null
            game.state.getEntity(game.state.stack.last())!!.get<SpellOnStackComponent>()!!
                .castOrdinalThisTurn shouldBe 3
            game.resolveStack()
            withClue("Lightning Bolt was countered; Shock still resolved") {
                game.getLifeTotal(1) shouldBe life1
                game.getLifeTotal(2) shouldBe 18
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }
        }

        test("the second spell is still the second after the first has resolved") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Second Guess", "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
