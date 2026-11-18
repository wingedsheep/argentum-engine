package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.ChariotOfVictory
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chariot of Victory (JOU #159) — {3} Artifact — Equipment.
 *
 *   Equipped creature has first strike, trample, and haste.
 *   Equip {1}
 */
class ChariotOfVictoryScenarioTest : ScenarioTestBase() {

    private val equipAbilityId = ChariotOfVictory.activatedAbilities.first().id

    init {
        test("equipping for {1} grants first strike, trample and haste, so a fresh creature can attack") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Chariot of Victory")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Chariot of Victory")!!,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the equipped creature has all three keywords") {
                projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets the summoning-sick Bears attack") {
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            }
        }
    }
}
