package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.PhyrexianArchivist
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Archivist (MOM #262) — "Reach / {2}, {T}: Put target card from a graveyard on the
 * bottom of its owner's library."
 *
 * The card goes to its *owner's* library whichever graveyard it came from, and the {T} limits the
 * ability to once per untap.
 */
class PhyrexianArchivistScenarioTest : ScenarioTestBase() {

    private val abilityId = PhyrexianArchivist.activatedAbilities.single().id

    init {
        context("Phyrexian Archivist") {

            test("puts an opponent's graveyard card on the bottom of their library, once per untap") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Phyrexian Archivist")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInLibrary(2, "Forest")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val archivist = game.findPermanent("Phyrexian Archivist")!!
                val opponent = game.player2Id
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = archivist,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bears, opponent, Zone.GRAVEYARD)),
                    )
                )
                withClue("activation: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears leaves the graveyard for the bottom of its owner's library") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.state.getLibrary(opponent).last() shouldBe bears
                }

                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = archivist,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bolt, opponent, Zone.GRAVEYARD)),
                    )
                )
                withClue("the tapped Archivist can't activate again") { second.error shouldNotBe null }
            }
        }
    }
}
