package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Mite Overseer (ONE #404) — {3}{W} Creature — Phyrexian Soldier 4/2.
 *
 * First strike
 * During your turn, creature tokens you control get +1/+0 and have first strike.
 * {3}{W/P}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 * "This token can't block."
 */
class MiteOverseerScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateOverseer() {
        val overseer = findPermanent("Mite Overseer")!!
        val abilityId = cardRegistry.getCard("Mite Overseer")!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = overseer, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("activated ability creates a Mite that is pumped and has first strike during your turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mite Overseer")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateOverseer()

            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 1
            val mite = mites.single()
            val overseer = game.findPermanent("Mite Overseer")!!
            val projected = game.state.projectedState

            withClue("token gets +1/+0 and first strike during your turn") {
                projected.getPower(mite) shouldBe 2
                projected.getToughness(mite) shouldBe 1
                projected.hasKeyword(mite, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(mite, Keyword.TOXIC) shouldBe true
            }
            withClue("Overseer itself is not a token, so it is not pumped") {
                projected.getPower(overseer) shouldBe 4
                projected.getToughness(overseer) shouldBe 2
                projected.hasKeyword(overseer, Keyword.FIRST_STRIKE) shouldBe true
            }
        }

        test("token loses the bonus during the opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mite Overseer")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateOverseer()
            val mite = game.findAllPermanents("Phyrexian Mite").single()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id

            val projected = game.state.projectedState
            projected.getPower(mite) shouldBe 1
            projected.getToughness(mite) shouldBe 1
            projected.hasKeyword(mite, Keyword.FIRST_STRIKE) shouldBe false
        }

        test("{W/P} can be paid with 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mite Overseer")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateOverseer()

            game.findAllPermanents("Phyrexian Mite") shouldHaveSize 1
            game.getLifeTotal(1) shouldBe 18
        }
    }
}
