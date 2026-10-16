package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.JaxisTheTroublemaker
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jaxis, the Troublemaker (SNC #112) — {R}, {T}, Discard a card: create a token copy of another
 * target creature you control. It gains haste and "When this token dies, draw a card." Sacrifice
 * it at the beginning of the next end step. Activate only as a sorcery.
 *
 * Covers that the haste and the dies-draw trigger are *granted* to the token (not baked into its
 * copiable values), that the end-step sacrifice fires the granted draw, and the discard cost.
 */
class JaxisTheTroublemakerScenarioTest : ScenarioTestBase() {

    private val copyAbility = JaxisTheTroublemaker.activatedAbilities.first().id

    private fun board(withDiscard: Boolean = true): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Jaxis, the Troublemaker")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withDiscard) builder.withCardInHand(1, "Forest")
        return builder.build()
    }

    init {
        context("Jaxis, the Troublemaker") {

            test("hasty copy with a granted dies-draw, sacrificed at the end step and drawing a card") {
                val game = board()
                val jaxis = game.findPermanent("Jaxis, the Troublemaker")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findCardsInHand(1, "Forest").single()

                game.execute(
                    ActivateAbility(
                        game.player1Id, jaxis, copyAbility,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Forest") shouldBe true
                game.handSize(1) shouldBe 0

                val token = game.findPermanents("Grizzly Bears").single { it != bears }
                withClue("the token has haste, but only as a granted ability — not a copiable value") {
                    game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
                    game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
                    game.state.getEntity(token)?.get<CardComponent>()?.baseKeywords
                        ?.contains(Keyword.HASTE) shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the token is sacrificed and its granted dies trigger draws a card") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                    game.handSize(1) shouldBe 1
                }
            }

            test("needs a card to discard, and can't target Jaxis itself") {
                val empty = board(withDiscard = false)
                val jaxis = empty.findPermanent("Jaxis, the Troublemaker")!!
                val bears = empty.findPermanent("Grizzly Bears")!!
                withClue("no card in hand to discard") {
                    empty.execute(
                        ActivateAbility(empty.player1Id, jaxis, copyAbility, targets = listOf(ChosenTarget.Permanent(bears)))
                    ).error shouldNotBe null
                }

                val game = board()
                val jaxis2 = game.findPermanent("Jaxis, the Troublemaker")!!
                val forest = game.findCardsInHand(1, "Forest").single()
                withClue("\"another\" excludes Jaxis") {
                    game.execute(
                        ActivateAbility(
                            game.player1Id, jaxis2, copyAbility,
                            targets = listOf(ChosenTarget.Permanent(jaxis2)),
                            costPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
