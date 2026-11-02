package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.grn.cards.LazavTheMultifarious
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lazav, the Multifarious (GRN #184).
 *
 * Focus: the copy exceptions on the becomes-a-copy path — the name stays "Lazav, the Multifarious",
 * the copy is legendary in addition to the copied card's types, and the retained ability lets Lazav
 * be re-aimed at a different mana value.
 */
class LazavTheMultifariousScenarioTest : ScenarioTestBase() {

    init {
        val copyAbilityId = LazavTheMultifarious.activatedAbilities[0].id

        context("Lazav, the Multifarious") {

            fun board() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Lazav, the Multifarious")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun ScenarioTestBase.TestGame.copyFromGraveyard(lazav: EntityId, x: Int, cardName: String) = execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = lazav,
                    abilityId = copyAbilityId,
                    xValue = x,
                    targets = listOf(
                        ChosenTarget.Card(
                            cardId = state.getGraveyard(player1Id).first { id ->
                                state.getEntity(id)?.get<CardComponent>()?.name == cardName
                            },
                            ownerId = player1Id,
                            zone = Zone.GRAVEYARD
                        )
                    )
                )
            )

            test("copies the graveyard creature but keeps its name, becomes legendary, and keeps this ability") {
                val game = board()
                val lazav = game.findPermanent("Lazav, the Multifarious")!!

                val result = game.copyFromGraveyard(lazav, x = 2, cardName = "Grizzly Bears")
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val card = game.state.getEntity(lazav)!!.get<CardComponent>()!!
                withClue("name is still Lazav") { card.name shouldBe "Lazav, the Multifarious" }
                withClue("P/T come from Grizzly Bears") {
                    game.state.projectedState.getPower(lazav) shouldBe 2
                    game.state.projectedState.getToughness(lazav) shouldBe 2
                }
                withClue("legendary in addition to Grizzly Bears' types") {
                    game.state.projectedState.isLegendary(lazav) shouldBe true
                    card.typeLine.subtypes.map { it.value } shouldBe listOf("Bear")
                }

                val again = game.copyFromGraveyard(lazav, x = 4, cardName = "Hill Giant")
                withClue("retained ability lets Lazav re-copy: ${again.error}") { again.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("now Hill Giant's 3/3, still named Lazav") {
                    game.state.getEntity(lazav)!!.get<CardComponent>()!!.name shouldBe "Lazav, the Multifarious"
                    game.state.projectedState.getPower(lazav) shouldBe 3
                    game.state.projectedState.getToughness(lazav) shouldBe 3
                }
            }

            test("a graveyard creature whose mana value isn't X is not a legal target") {
                val game = board()
                val lazav = game.findPermanent("Lazav, the Multifarious")!!
                val result = game.copyFromGraveyard(lazav, x = 2, cardName = "Hill Giant")
                withClue("mismatched mana value must be rejected") { (result.error != null) shouldBe true }
            }
        }
    }
}
