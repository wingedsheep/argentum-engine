package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Arclight Phoenix (GRN #91).
 *
 * "Flying, haste / At the beginning of combat on your turn, if you've cast three or more instant and
 *  sorcery spells this turn, return this card from your graveyard to the battlefield."
 *
 * Pinned: three instants/sorceries bring it back at beginning of combat in time to attack (haste);
 * a creature spell doesn't count toward the three.
 */
class ArclightPhoenixScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Phoenix", "Opponent")
        .withCardInGraveyard(1, "Arclight Phoenix")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Lava Spike")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Forest", 2)
        .apply { repeat(3) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.bolt(name: String) {
        castSpellTargetingPlayer(1, name, 2).error shouldBe null
        resolveStack()
    }

    init {
        test("three instant and sorcery spells return it at beginning of combat, and it can attack") {
            val game = board()
            game.bolt("Lightning Bolt")
            game.bolt("Lightning Bolt")
            game.bolt("Lava Spike")

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            withClue("the graveyard trigger is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.isOnBattlefield("Arclight Phoenix") shouldBe true
            game.isInGraveyard(1, "Arclight Phoenix") shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets it attack the combat it returns") {
                game.declareAttackers(mapOf("Arclight Phoenix" to 2)).error shouldBe null
            }
        }

        test("a creature spell doesn't count toward the three") {
            val game = board()
            game.bolt("Lightning Bolt")
            game.bolt("Lightning Bolt")
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.state.stack.size shouldBe 0
            game.isInGraveyard(1, "Arclight Phoenix") shouldBe true
        }
    }
}
