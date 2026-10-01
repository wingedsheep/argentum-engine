package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Minor Misstep ({U}, Instant): "Counter target spell with mana value 1 or less."
 */
class MinorMisstepScenarioTest : ScenarioTestBase() {

    private fun setup(spell: String, land: String, lands: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, spell)
        .withCardInHand(2, "Minor Misstep")
        .withLandsOnBattlefield(1, land, lands)
        .withLandsOnBattlefield(2, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castMisstepAt(spellName: String) = run {
        val spellOnStack = state.stack.first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == spellName
        }
        val misstepId = state.getHand(player2Id).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Minor Misstep"
        }
        execute(CastSpell(player2Id, misstepId, listOf(ChosenTarget.Spell(spellOnStack))))
    }

    init {
        context("Minor Misstep") {
            test("counters a spell with mana value 1") {
                val game = setup("Llanowar Elves", "Forest", 1)
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.execute(PassPriority(game.player1Id))

                val result = game.castMisstepAt("Llanowar Elves")
                withClue("Minor Misstep should be castable: ${result.error}") {
                    result.error shouldBe null
                }

                var iterations = 0
                while (game.state.stack.isNotEmpty() && iterations < 20) {
                    val p = game.state.priorityPlayerId ?: break
                    if (game.execute(PassPriority(p)).error != null) break
                    iterations++
                }

                game.isInGraveyard(1, "Llanowar Elves") shouldBe true
                game.isOnBattlefield("Llanowar Elves") shouldBe false
                game.isInGraveyard(2, "Minor Misstep") shouldBe true
            }

            test("cannot target a spell with mana value 2") {
                val game = setup("Grizzly Bears", "Forest", 2)
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.execute(PassPriority(game.player1Id))

                val result = game.castMisstepAt("Grizzly Bears")
                withClue("Minor Misstep must reject an MV-2 spell as a target") {
                    result.error.shouldNotBeNull()
                }
            }
        }
    }
}
