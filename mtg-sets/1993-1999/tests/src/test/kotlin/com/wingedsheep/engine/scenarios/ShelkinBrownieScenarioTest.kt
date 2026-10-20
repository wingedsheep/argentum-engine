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

/**
 * Shelkin Brownie (LEG #204) — {1}{G} Creature — Ouphe 1/1:
 * "{T}: Target creature loses all 'bands with other' abilities until end of turn."
 *
 * First card to lower `Effects.RemoveAllBandsWithOther`: a granted "bands with other legendary
 * creatures" (Cathedral of Serra) is stripped, while plain banding survives the same effect.
 */
class ShelkinBrownieScenarioTest : ScenarioTestBase() {

    private val bandsWithOtherLegendary =
        KeywordAbility.bandsWithOther(Supertype.LEGENDARY).projectedKeyword

    private fun TestGame.activateBrownie(target: EntityId) =
        execute(
            ActivateAbility(
                playerId = player2Id,
                sourceId = findPermanent("Shelkin Brownie")!!,
                abilityId = cardRegistry.getCard("Shelkin Brownie")!!.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )

    init {
        context("Shelkin Brownie") {

            test("strips a granted 'bands with other' ability until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathedral of Serra")
                    .withCardOnBattlefield(1, "Kasimir the Lone Wolf")
                    .withCardOnBattlefield(2, "Shelkin Brownie")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kasimir = game.findPermanent("Kasimir the Lone Wolf")!!
                game.state.projectedState.hasKeyword(kasimir, bandsWithOtherLegendary) shouldBe true

                game.activateBrownie(kasimir).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(kasimir, bandsWithOtherLegendary) shouldBe false
            }

            test("leaves plain banding in place") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pikemen")
                    .withCardOnBattlefield(2, "Shelkin Brownie")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pikemen = game.findPermanent("Pikemen")!!
                game.activateBrownie(pikemen).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(pikemen, Keyword.BANDING) shouldBe true
            }
        }
    }
}
