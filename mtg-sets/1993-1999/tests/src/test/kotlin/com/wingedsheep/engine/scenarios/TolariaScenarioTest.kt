package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tolaria (LEG #308) — Legendary Land:
 * "{T}: Add {U}.
 *  {T}: Target creature loses banding and all 'bands with other' abilities until end of turn.
 *  Activate only during any upkeep step."
 *
 * Covers: removing printed banding (the creature keeps its other keywords), removing a
 * "bands with other" ability granted by a static (Cathedral of Serra) during an opponent's
 * upkeep, and the upkeep-only activation window.
 */
class TolariaScenarioTest : ScenarioTestBase() {

    private val removeAbilityId by lazy {
        cardRegistry.getCard("Tolaria")!!.activatedAbilities[1].id
    }

    private val bandsWithOtherLegendary =
        KeywordAbility.bandsWithOther(Supertype.LEGENDARY).projectedKeyword

    private fun TestGame.activateTolaria(playerNumber: Int, target: EntityId) =
        execute(
            ActivateAbility(
                playerId = if (playerNumber == 1) player1Id else player2Id,
                sourceId = findPermanent("Tolaria")!!,
                abilityId = removeAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )

    init {
        context("Tolaria") {

            test("during your upkeep, a creature loses banding but keeps its other keywords") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tolaria")
                    .withCardOnBattlefield(1, "Pikemen")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val pikemen = game.findPermanent("Pikemen")!!
                game.state.projectedState.hasKeyword(pikemen, Keyword.BANDING) shouldBe true

                game.activateTolaria(1, pikemen).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.hasKeyword(pikemen, Keyword.BANDING) shouldBe false
                projected.hasKeyword(pikemen, Keyword.FIRST_STRIKE) shouldBe true
            }

            test("during an opponent's upkeep, strips a granted 'bands with other' ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(2, "Tolaria")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val kasimir = game.findPermanent("Kasimir the Lone Wolf")!!
                game.state.projectedState.hasKeyword(kasimir, bandsWithOtherLegendary) shouldBe true

                game.activateTolaria(2, kasimir).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(kasimir, bandsWithOtherLegendary) shouldBe false
            }

            test("can't be activated outside an upkeep step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tolaria")
                    .withCardOnBattlefield(1, "Pikemen")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateTolaria(1, game.findPermanent("Pikemen")!!).error shouldNotBe null
            }
        }
    }
}
