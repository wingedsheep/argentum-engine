package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scheming Fence (SNC #219) — as it enters you may choose a nonland permanent; that permanent's
 * activated abilities can't be activated, and the Fence has them (except loyalty abilities),
 * payable with mana as though it were mana of any color.
 */
class SchemingFenceScenarioTest : ScenarioTestBase() {

    private val shadePump by lazy { cardRegistry.requireCard("Frozen Shade").activatedAbilities.single().id }

    init {
        context("Scheming Fence") {
            test("cast: choose an opponent's Frozen Shade, then pump the Fence with white mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scheming Fence")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Frozen Shade")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shade = game.findPermanent("Frozen Shade")!!
                game.castSpell(1, "Scheming Fence").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("nonland permanents only") {
                    decision.options shouldContain shade
                    decision.options shouldNotContain game.findPermanent("Swamp")!!
                }
                game.selectCards(listOf(shade)).error shouldBe null

                val fence = game.findPermanent("Scheming Fence")!!
                game.state.chosenCreatureOf(fence) shouldBe shade
                game.execute(ActivateAbility(game.player1Id, fence, shadePump)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(fence) shouldBe 3
                game.state.projectedState.getToughness(fence) shouldBe 4
                game.state.projectedState.getPower(shade) shouldBe 0
            }

            test("returned by Reanimate, the Fence may decline the choice and steals nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Reanimate")
                    .withCardInGraveyard(1, "Scheming Fence")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Frozen Shade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Scheming Fence").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection().error shouldBe null

                val fence = game.findPermanent("Scheming Fence")!!
                game.state.chosenCreatureOf(fence) shouldBe null
                game.execute(ActivateAbility(game.player1Id, fence, shadePump)).error shouldNotBe null
            }
        }
    }
}
