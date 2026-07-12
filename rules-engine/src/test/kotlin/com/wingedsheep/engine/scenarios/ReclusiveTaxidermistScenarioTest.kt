package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Reclusive Taxidermist (VOW #214) — {1}{G} Creature — Human Druid, 1/2.
 *
 *   This creature gets +3/+2 as long as there are four or more creature cards in your graveyard.
 *   {T}: Add one mana of any color.
 *
 * Exercises the conditional +3/+2 static ability gated on the graveyard creature-card count, and
 * the {T}: add one mana of any color activated ability.
 */
class ReclusiveTaxidermistScenarioTest : ScenarioTestBase() {

    init {
        context("Reclusive Taxidermist") {

            test("base stats 1/2 with fewer than four creature cards in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reclusive Taxidermist", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taxidermist = game.findPermanent("Reclusive Taxidermist")!!
                withClue("3 creature cards in graveyard < 4 -> base 1/2") {
                    game.state.projectedState.getPower(taxidermist) shouldBe 1
                    game.state.projectedState.getToughness(taxidermist) shouldBe 2
                }
            }

            test("gets +3/+2 (becomes 4/4) with four or more creature cards in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reclusive Taxidermist", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taxidermist = game.findPermanent("Reclusive Taxidermist")!!
                withClue("4 creature cards in graveyard >= 4 -> boosted to 4/4") {
                    game.state.projectedState.getPower(taxidermist) shouldBe 4
                    game.state.projectedState.getToughness(taxidermist) shouldBe 4
                }
            }

            test("{T}: add one mana of any color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reclusive Taxidermist", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taxidermist = game.findPermanent("Reclusive Taxidermist")!!
                val ability = cardRegistry.getCard("Reclusive Taxidermist")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = taxidermist,
                        abilityId = ability.id,
                        manaColorChoice = Color.GREEN
                    )
                )
                withClue("Activating the mana ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("one green mana was added to the pool") {
                    (pool?.green ?: 0) shouldBe 1
                }
            }
        }
    }
}
