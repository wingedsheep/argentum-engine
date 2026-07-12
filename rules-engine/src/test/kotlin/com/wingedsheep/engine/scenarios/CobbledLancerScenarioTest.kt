package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Cobbled Lancer (VOW #52) — {U} Creature — Zombie Horse, 3/3.
 *
 *   As an additional cost to cast this spell, exile a creature card from your graveyard.
 *   {3}{U}, Exile this card from your graveyard: Draw a card.
 *
 * Exercises the additional cast cost (must exile a creature card from the graveyard to cast it at
 * all) and the graveyard-activated draw-a-card ability.
 */
class CobbledLancerScenarioTest : ScenarioTestBase() {

    init {
        context("Cobbled Lancer") {

            test("casting it exiles a creature card from the graveyard as an additional cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cobbled Lancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lancerCard = game.findCardsInHand(1, "Cobbled Lancer").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = lancerCard,
                        targets = emptyList(),
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(bears))
                    )
                )
                withClue("casting should succeed while exiling a creature card: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears was exiled to pay the additional cost") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
                withClue("Cobbled Lancer resolved onto the battlefield") {
                    game.isOnBattlefield("Cobbled Lancer") shouldBe true
                }
            }

            test("cannot be cast without a creature card in the graveyard to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cobbled Lancer")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Cobbled Lancer")
                withClue("casting must fail — the additional cost requires a creature card to exile") {
                    (cast.error != null) shouldBe true
                }
                withClue("Cobbled Lancer stays in hand") {
                    game.isInHand(1, "Cobbled Lancer") shouldBe true
                }
            }

            test("exiling it from the graveyard for {3}{U} draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Cobbled Lancer")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lancer = game.findCardsInGraveyard(1, "Cobbled Lancer").single()
                val abilityId = cardRegistry.getCard("Cobbled Lancer")!!.activatedAbilities.first().id
                val handSizeBefore = game.handSize(1)

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lancer,
                        abilityId = abilityId
                    )
                )
                withClue("activation from the graveyard should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("the card is exiled from the graveyard") {
                    game.isInGraveyard(1, "Cobbled Lancer") shouldBe false
                    game.isInExile(1, "Cobbled Lancer") shouldBe true
                }
                withClue("a card was drawn") {
                    game.handSize(1) shouldBe handSizeBefore + 1
                }
            }
        }
    }
}
