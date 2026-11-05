package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.ScribeOfTheMindful
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scribe of the Mindful (AKH #68) — {2}{U} Creature — Human Cleric, 2/2.
 *
 *   {1}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard
 *   to your hand.
 */
class ScribeOfTheMindfulScenarioTest : ScenarioTestBase() {

    private val abilityId = ScribeOfTheMindful.activatedAbilities.first().id

    init {
        context("Scribe of the Mindful") {

            test("sacrifices itself to return an instant from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scribe of the Mindful")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val scribe = game.findPermanent("Scribe of the Mindful")!!
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scribe,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bolt, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null

                withClue("the Scribe is sacrificed as part of the cost") {
                    game.isOnBattlefield("Scribe of the Mindful") shouldBe false
                    game.isInGraveyard(1, "Scribe of the Mindful") shouldBe true
                }

                game.resolveStack()

                withClue("Lightning Bolt returns to its owner's hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                }
            }

            test("can't target a creature card in your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Scribe of the Mindful")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val scribe = game.findPermanent("Scribe of the Mindful")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scribe,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldNotBe null
                game.isOnBattlefield("Scribe of the Mindful") shouldBe true
            }
        }
    }
}
