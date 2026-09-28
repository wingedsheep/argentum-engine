package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Phyrexian Awakening — "When this enchantment enters, incubate 4. Phyrexians you control have
 * vigilance."
 */
class PhyrexianAwakeningScenarioTest : ScenarioTestBase() {
    init {
        context("Phyrexian Awakening") {
            test("incubates 4 and grants vigilance only to Phyrexians you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Phyrexian Awakening")
                    .withCardOnBattlefield(1, "Infected Defector")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Infected Defector")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Phyrexian Awakening").error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
                game.state.getBattlefield(game.player1Id).contains(incubator) shouldBe true

                val projected = game.state.projectedState
                val myDefector = game.state.getBattlefield(game.player1Id)
                    .first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Infected Defector" }
                val theirDefector = game.state.getBattlefield(game.player2Id)
                    .first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Infected Defector" }
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("your Phyrexian has vigilance") { projected.hasKeyword(myDefector, Keyword.VIGILANCE) shouldBe true }
                withClue("an opponent's Phyrexian doesn't") { projected.hasKeyword(theirDefector, Keyword.VIGILANCE) shouldBe false }
                withClue("a non-Phyrexian doesn't") { projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false }
                withClue("the untransformed Incubator isn't a Phyrexian") {
                    projected.hasKeyword(incubator, Keyword.VIGILANCE) shouldBe false
                }

                val abilityId = cardRegistry.getCard("Incubator")!!.activatedAbilities.first().id
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = incubator, abilityId = abilityId)).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the transformed Phyrexian token has vigilance") {
                    game.state.projectedState.hasKeyword(incubator, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
