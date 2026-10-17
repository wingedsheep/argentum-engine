package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.UnlicensedHearse
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unlicensed Hearse (SNC #246) — {2} Artifact — Vehicle, * / *
 *
 *   {T}: Exile up to two target cards from a single graveyard.
 *   Unlicensed Hearse's power and toughness are each equal to the number of cards exiled with it.
 *   Crew 2
 *
 * Proves the exiled cards are linked to the Hearse and its characteristic-defining P/T counts them
 * and that the two targets must share a graveyard.
 */
class UnlicensedHearseScenarioTest : ScenarioTestBase() {

    private val exileAbilityId by lazy { UnlicensedHearse.script.activatedAbilities[0].id }

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Unlicensed Hearse", summoningSickness = false)
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Savannah Lions")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.graveyardTarget(playerNumber: Int, name: String): ChosenTarget.Card {
        val ownerId = if (playerNumber == 1) player1Id else player2Id
        return ChosenTarget.Card(findCardsInGraveyard(playerNumber, name).single(), ownerId, Zone.GRAVEYARD)
    }

    init {
        context("Unlicensed Hearse") {

            test("exiling two cards from one graveyard makes it 2/2") {
                val game = board()
                val hearse = game.findPermanent("Unlicensed Hearse")!!
                withClue("no cards exiled with it yet: 0/0") {
                    game.state.projectedState.getPower(hearse) shouldBe 0
                    game.state.projectedState.getToughness(hearse) shouldBe 0
                }

                val bears = game.graveyardTarget(2, "Grizzly Bears")
                val lions = game.graveyardTarget(2, "Savannah Lions")
                game.execute(
                    ActivateAbility(game.player1Id, hearse, exileAbilityId, listOf(bears, lions))
                ).error shouldBe null
                game.resolveStack()

                val opponentExile = game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE))
                withClue("both of the opponent's cards were exiled") {
                    opponentExile.containsAll(listOf(bears.cardId, lions.cardId)) shouldBe true
                }
                withClue("two cards exiled with it: 2/2") {
                    game.state.projectedState.getPower(hearse) shouldBe 2
                    game.state.projectedState.getToughness(hearse) shouldBe 2
                }
            }

            test("the two targets must come from a single graveyard") {
                val game = board()
                val hearse = game.findPermanent("Unlicensed Hearse")!!
                withClue("Hill Giant (Player1's graveyard) and Grizzly Bears (Player2's) can't be targeted together") {
                    game.execute(
                        ActivateAbility(
                            game.player1Id, hearse, exileAbilityId,
                            listOf(game.graveyardTarget(1, "Hill Giant"), game.graveyardTarget(2, "Grizzly Bears"))
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
