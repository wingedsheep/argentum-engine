package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shigeki, Jukai Visionary (NEO #206) — {1}{G} Legendary Enchantment Creature — Snake Druid 1/3.
 *
 *   {1}{G}, {T}, Return Shigeki to its owner's hand: Reveal the top four cards of your library. You
 *   may put a land card from among them onto the battlefield tapped. Put the rest into your
 *   graveyard.
 *   Channel — {X}{X}{G}{G}, Discard this card: Return X target nonlegendary cards from your
 *   graveyard to your hand.
 *
 * Covers the return-self cost (Shigeki back in hand, ability still resolves), the land-to-
 * battlefield-tapped / rest-to-graveyard split, and the channel's X-target count on a from-hand
 * activation whose cost pays {X} twice.
 */
class ShigekiJukaiVisionaryScenarioTest : ScenarioTestBase() {

    private fun battlefieldAbilityId() = cardRegistry.getCard("Shigeki, Jukai Visionary")!!
        .activatedAbilities.first { it.activateFromZone != Zone.HAND }.id

    private fun channelAbilityId() = cardRegistry.getCard("Shigeki, Jukai Visionary")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    private fun ScenarioTestBase.TestGame.graveyardCard(name: String): EntityId =
        state.getGraveyard(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Shigeki, Jukai Visionary") {

            test("returns itself as a cost, puts a revealed land onto the battlefield tapped, mills the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shigeki, Jukai Visionary")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shigeki = game.findPermanent("Shigeki, Jukai Visionary").shouldNotBeNull()
                val island = game.state.getZone(game.player1Id, Zone.LIBRARY)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Island" }

                val result = game.execute(ActivateAbility(game.player1Id, shigeki, battlefieldAbilityId()))
                withClue("{1}{G} from two Forests: ${result.error}") { result.error shouldBe null }

                withClue("returning Shigeki is part of the cost") {
                    game.isInHand(1, "Shigeki, Jukai Visionary") shouldBe true
                }

                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(island))

                withClue("the chosen land enters tapped") {
                    game.state.getBattlefield().contains(island) shouldBe true
                    game.state.getEntity(island)?.has<TappedComponent>() shouldBe true
                }
                withClue("the rest — including the unchosen land — goes to the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Mountain") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.librarySize(1) shouldBe 0
                }
            }

            test("channel with X = 2 returns exactly two target nonlegendary cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shigeki, Jukai Visionary")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handCard = game.findCardsInHand(1, "Shigeki, Jukai Visionary").first()
                val bears = game.graveyardCard("Grizzly Bears")
                val giant = game.graveyardCard("Hill Giant")

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, handCard, channelAbilityId(),
                        targets = listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)
                        ),
                        xValue = 2
                    )
                )
                withClue("{2}{2}{G}{G} from six Forests: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Island") shouldBe true
                withClue("Shigeki was discarded as the cost") {
                    game.isInGraveyard(1, "Shigeki, Jukai Visionary") shouldBe true
                }
            }

            test("channel can't target a legendary card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shigeki, Jukai Visionary")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Boseiju, Who Endures")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handCard = game.findCardsInHand(1, "Shigeki, Jukai Visionary").first()
                val boseiju = game.graveyardCard("Boseiju, Who Endures")

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, handCard, channelAbilityId(),
                        targets = listOf(ChosenTarget.Card(boseiju, game.player1Id, Zone.GRAVEYARD)),
                        xValue = 1
                    )
                )
                result.error shouldNotBe null
                game.isInHand(1, "Shigeki, Jukai Visionary") shouldBe true
            }
        }
    }
}
