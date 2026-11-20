package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.RotFarmSkeleton
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rot Farm Skeleton (DGM #98) — {2}{B}{G} 4/1 Creature — Plant Skeleton.
 *
 *   This creature can't block.
 *   {2}{B}{G}, Mill four cards: Return this card from your graveyard to the battlefield.
 *   Activate only as a sorcery.
 */
class RotFarmSkeletonScenarioTest : ScenarioTestBase() {
    init {
        test("pays {2}{B}{G} and mills four to return itself from the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Rot Farm Skeleton")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val skeleton = game.findCardsInGraveyard(1, "Rot Farm Skeleton").single()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = skeleton,
                    abilityId = RotFarmSkeleton.activatedAbilities.single().id,
                )
            ).error shouldBe null

            withClue("the mill is a cost: four cards are milled on activation") {
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 5 // the Skeleton still there, plus four Islands
            }

            game.resolveStack()

            withClue("the Skeleton returned to the battlefield") {
                game.isOnBattlefield("Rot Farm Skeleton") shouldBe true
                game.isInGraveyard(1, "Rot Farm Skeleton") shouldBe false
            }
        }

        test("it can't block") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rot Farm Skeleton")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("blocking with Rot Farm Skeleton is illegal") {
                game.declareBlockers(mapOf("Rot Farm Skeleton" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
        }
    }
}
