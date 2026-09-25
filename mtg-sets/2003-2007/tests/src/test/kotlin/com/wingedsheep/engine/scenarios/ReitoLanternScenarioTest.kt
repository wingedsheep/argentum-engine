package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.ReitoLantern
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reito Lantern (CHK #267) — "{3}: Put target card from a graveyard on the bottom of its owner's
 * library."
 *
 * No {T} in the cost, so the Lantern can be activated more than once a turn; and the card goes to
 * its *owner's* library, whichever graveyard it was in.
 */
class ReitoLanternScenarioTest : ScenarioTestBase() {

    private val abilityId = ReitoLantern.activatedAbilities.single().id

    init {
        context("Reito Lantern") {

            test("puts an opponent's graveyard card on the bottom of their library, twice in a turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reito Lantern")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInLibrary(2, "Forest")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lantern = game.findPermanent("Reito Lantern")!!
                val opponent = game.player2Id

                for (name in listOf("Grizzly Bears", "Lightning Bolt")) {
                    val card = game.findCardsInGraveyard(2, name).single()
                    val activation = game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = lantern,
                            abilityId = abilityId,
                            targets = listOf(ChosenTarget.Card(card, opponent, Zone.GRAVEYARD)),
                        )
                    )
                    withClue("activating for $name: ${activation.error}") { activation.error shouldBe null }
                    game.resolveStack()

                    withClue("$name leaves the graveyard for the bottom of its owner's library") {
                        game.isInGraveyard(2, name) shouldBe false
                        game.state.getLibrary(opponent).last() shouldBe card
                    }
                }
            }
        }
    }
}
