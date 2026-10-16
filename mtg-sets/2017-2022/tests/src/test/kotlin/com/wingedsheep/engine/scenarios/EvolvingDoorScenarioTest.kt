package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.EvolvingDoor
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Evolving Door (SNC #144, {2}{G}, Artifact).
 *
 *   {1}, {T}, Sacrifice a creature: Count the colors of the sacrificed creature, then search your
 *   library for a creature card that's exactly that many colors plus one. Exile that card, then
 *   shuffle. You may cast the exiled card. Activate only as a sorcery.
 */
class EvolvingDoorScenarioTest : ScenarioTestBase() {

    private val ability = EvolvingDoor.activatedAbilities.single().id

    init {
        context("Evolving Door") {

            test("a one-color sacrifice finds only a two-color creature card; declining leaves it exiled") {
                val game = board(sacrifice = "Grizzly Bears")
                activate(game, "Grizzly Bears")

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the two-color card is exactly 1 + 1 colors") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Boros Swiftblade")
                }
                game.selectCards(search.options)

                game.answerYesNo(false)
                game.resolveStack()

                game.isInExile(1, "Boros Swiftblade").shouldBeTrue()
                game.isInGraveyard(1, "Grizzly Bears").shouldBeTrue()
                withClue("an uncast card isn't castable later") {
                    game.getLegalActions(1).any {
                        it.actionType == "CastSpell" && it.description.contains("Boros Swiftblade")
                    }.shouldBeFalse()
                }
            }

            test("a colorless sacrifice finds a monocolored creature card, which can be cast by paying its cost") {
                val game = board(sacrifice = "Memnite")
                activate(game, "Memnite")

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("zero colors plus one is exactly one color") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Grizzly Bears")
                }
                game.selectCards(search.options)

                game.answerYesNo(true)
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("cast during resolution, ignoring creature timing") {
                    game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                }
                game.isInExile(1, "Grizzly Bears").shouldBeFalse()
            }

            test("a sacrificed token is counted as it last existed: two colors find a three-color card") {
                val game = board(sacrifice = "Boros Swiftblade", sacrificeIsToken = true)
                activate(game, "Boros Swiftblade")

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the token has ceased to exist, but its last-known colors are red and white") {
                    search.options.map { game.cardName(it) } shouldBe listOf("Woolly Thoctar")
                }
            }
        }
    }

    private fun board(sacrifice: String, sacrificeIsToken: Boolean = false): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Evolving Door")
            .withCardOnBattlefield(1, sacrifice, isToken = sacrificeIsToken)
            .withCardInLibrary(1, "Memnite")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Boros Swiftblade")
            .withCardInLibrary(1, "Woolly Thoctar")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    private fun activate(game: TestGame, sacrifice: String) {
        val door = game.findPermanent("Evolving Door")!!
        val victim = game.findPermanent(sacrifice)!!
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = door,
                abilityId = ability,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim)),
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).error shouldBe null
        game.resolveStack()
    }

    private fun TestGame.cardName(id: com.wingedsheep.sdk.model.EntityId): String? =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
}
