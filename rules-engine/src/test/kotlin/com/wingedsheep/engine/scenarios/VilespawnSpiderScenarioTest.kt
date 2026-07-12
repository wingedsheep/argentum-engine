package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vilespawn Spider (VOW #250) — {G}{U} Creature — Spider, 2/3, reach.
 *
 *   At the beginning of your upkeep, mill a card.
 *   {2}{G}{U}, {T}, Sacrifice this creature: Create a 1/1 green Insect creature token for each
 *   creature card in your graveyard. Activate only as a sorcery.
 *
 * Exercises both abilities: the upkeep mill trigger, and the sacrifice ability creating one
 * Insect token per creature card already in the graveyard.
 */
class VilespawnSpiderScenarioTest : ScenarioTestBase() {

    init {
        context("Vilespawn Spider") {

            test("has reach and enters as a 2/3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vilespawn Spider")
                    .withActivePlayer(1)
                    .build()

                val spider = game.findPermanent("Vilespawn Spider")!!
                withClue("2/3 with reach") {
                    game.state.projectedState.getPower(spider) shouldBe 2
                    game.state.projectedState.getToughness(spider) shouldBe 3
                    game.state.projectedState.hasKeyword(spider, Keyword.REACH) shouldBe true
                }
            }

            test("upkeep trigger mills exactly one card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vilespawn Spider")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val librarySizeBefore = game.librarySize(1)
                val graveyardSizeBefore = game.graveyardSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("upkeep mills exactly one card") {
                    game.librarySize(1) shouldBe librarySizeBefore - 1
                    game.graveyardSize(1) shouldBe graveyardSizeBefore + 1
                }
            }

            test("sacrifice ability creates a 1/1 Insect token for each creature card in the graveyard, counting itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vilespawn Spider", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Lightning Bolt") // not a creature card — excluded
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Vilespawn Spider")!!
                val abilityId = cardRegistry.getCard("Vilespawn Spider")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spider,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the sacrifice ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Vilespawn Spider was sacrificed") {
                    game.isOnBattlefield("Vilespawn Spider") shouldBe false
                    game.isInGraveyard(1, "Vilespawn Spider") shouldBe true
                }
                withClue(
                    "Sacrifice is paid on activation, so the Spider is already in the graveyard when " +
                        "the effect counts creature cards: Grizzly Bears + Hill Giant + Vilespawn Spider " +
                        "= 3 Insect tokens (Lightning Bolt is not a creature card)"
                ) {
                    game.findPermanents("Insect Token").size shouldBe 3
                }
            }
        }
    }
}
