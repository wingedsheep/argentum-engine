package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Balan, Wandering Knight (C17 #2) — {2}{W}{W} Legendary Creature — Cat Knight, 3/3.
 *
 *   First strike
 *   Balan has double strike as long as two or more Equipment are attached to it.
 *   {1}{W}: Attach all Equipment you control to Balan.
 */
class BalanWanderingKnightScenarioTest : ScenarioTestBase() {

    private val balanName = "Balan, Wandering Knight"

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    private fun TestGame.activateBalan() {
        val balan = findPermanent(balanName)!!
        val abilityId = cardRegistry.getCard(balanName)!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = balan, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Balan, Wandering Knight") {

            test("with no Equipment Balan has first strike but not double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, balanName)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balan = game.findPermanent(balanName)!!
                game.state.projectedState.hasKeyword(balan, Keyword.FIRST_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(balan, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            test("attaches all Equipment you control — even from another creature — and gains double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, balanName)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardAttachedTo(1, "Leonin Scimitar", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Short Sword")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balan = game.findPermanent(balanName)!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                val scimitar = game.findPermanent("Leonin Scimitar")!!
                val theirSword = game.findPermanent("Short Sword")!!
                game.attachedTo(scimitar) shouldBe game.findPermanent("Grizzly Bears")

                game.activateBalan()

                withClue("both of your Equipment are now on Balan") {
                    game.attachedTo(bonesplitter) shouldBe balan
                    game.attachedTo(scimitar) shouldBe balan
                }
                withClue("the opponent's Equipment is not 'Equipment you control'") {
                    game.attachedTo(theirSword) shouldBe null
                }
                withClue("two Equipment attached: double strike, and the equip bonuses apply (3 + 2 + 1)") {
                    game.state.projectedState.hasKeyword(balan, Keyword.DOUBLE_STRIKE) shouldBe true
                    game.state.projectedState.getPower(balan) shouldBe 6
                }
            }

            test("a single attached Equipment is not enough for double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, balanName)
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balan = game.findPermanent(balanName)!!
                game.activateBalan()

                game.attachedTo(game.findPermanent("Bonesplitter")!!) shouldBe balan
                game.state.projectedState.getPower(balan) shouldBe 5
                game.state.projectedState.hasKeyword(balan, Keyword.FIRST_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(balan, Keyword.DOUBLE_STRIKE) shouldBe false
            }
        }
    }
}
