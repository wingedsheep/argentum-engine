package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Purphoros's Intervention (THB #151) — {X}{R} Sorcery, choose one:
 * create an X/1 red Elemental token with trample and haste that's sacrificed at the next end step,
 * or deal twice X damage to target creature or planeswalker.
 *
 * Pins the token's power to the X paid (fixed at creation), its end-step sacrifice, and the damage
 * mode's doubled X: X = 2 kills a 3/3, which X damage alone would not.
 */
class PurphorossInterventionScenarioTest : ScenarioTestBase() {

    private val tokenMode = 0
    private val damageMode = 1

    init {
        test("token mode creates an X/1 trample haste Elemental that's sacrificed at the next end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Purphoros's Intervention")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Purphoros's Intervention").single()
            game.execute(
                CastSpell(
                    game.player1Id, spell, emptyList(),
                    xValue = 3,
                    chosenModes = listOf(tokenMode),
                )
            ).error shouldBe null
            game.resolveStack()

            val token = game.findPermanent("Elemental Token").shouldNotBeNull()
            game.state.projectedState.getPower(token) shouldBe 3
            game.state.projectedState.getToughness(token) shouldBe 1
            game.state.projectedState.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
            game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanent("Elemental Token") shouldBe null
        }

        test("damage mode deals twice X damage to the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Purphoros's Intervention")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Purphoros's Intervention").single()
            val target = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
            game.execute(
                CastSpell(
                    game.player1Id, spell, target,
                    xValue = 2,
                    chosenModes = listOf(damageMode),
                    modeTargetsOrdered = listOf(target),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
