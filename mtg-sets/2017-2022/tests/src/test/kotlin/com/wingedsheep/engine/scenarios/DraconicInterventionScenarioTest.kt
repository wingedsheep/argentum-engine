package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Draconic Intervention (STX #96) — {2}{R}{R} Sorcery.
 *
 *   As an additional cost to cast this spell, exile an instant or sorcery card from your graveyard.
 *   Draconic Intervention deals X damage to each non-Dragon creature, where X is the exiled card's
 *   mana value. If a creature dealt damage this way would die this turn, exile it instead.
 *   Exile Draconic Intervention.
 *
 * Covers: X read off the exiled card, Dragons spared, creatures killed by the damage exiled, a
 * damaged survivor exiled when it dies later in the turn, an undamaged Dragon dying normally later
 * in the turn (only creatures actually dealt damage are marked), and the spell exiling itself.
 */
class DraconicInterventionScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Draconic Intervention")
        .withCardInHand(1, "Shock")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInGraveyard(1, "Cancel") // mana value 3
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
        .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // 6/4
        .withCardOnBattlefield(2, "Dragon Whelp", summoningSickness = false) // 2/3 Dragon
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Draconic Intervention") {

            test("deals the exiled card's mana value to non-Dragons and exiles what it damaged when it dies this turn") {
                val game = board()
                val cancel = game.findCardsInGraveyard(1, "Cancel").first()
                val card = game.findCardsInHand(1, "Draconic Intervention").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(cancel)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the cost exiled Cancel, and the spell exiled itself") {
                    game.isInExile(1, "Cancel") shouldBe true
                    game.isInExile(1, "Draconic Intervention") shouldBe true
                }
                withClue("Grizzly Bears died to 3 damage and was exiled instead") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("Craw Wurm (6/4) survives 3 damage") {
                    game.findPermanent("Craw Wurm") shouldNotBe null
                }
                withClue("the Dragon took no damage") {
                    game.findPermanent("Dragon Whelp") shouldNotBe null
                }

                // Later this turn: the damaged Wurm dies to Shock -> exiled instead.
                game.castSpell(1, "Shock", game.findPermanent("Craw Wurm")).error shouldBe null
                game.resolveStack()
                withClue("a creature dealt damage this way that dies later this turn is exiled") {
                    game.isInExile(2, "Craw Wurm") shouldBe true
                    game.isInGraveyard(2, "Craw Wurm") shouldBe false
                }

                // The undamaged Dragon dies normally.
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Dragon Whelp")).error shouldBe null
                game.resolveStack()
                withClue("a creature not dealt damage this way goes to the graveyard as usual") {
                    game.isInGraveyard(2, "Dragon Whelp") shouldBe true
                    game.isInExile(2, "Dragon Whelp") shouldBe false
                }
            }

            test("cannot be cast without an instant or sorcery card to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Draconic Intervention")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Draconic Intervention").first()

                game.execute(CastSpell(game.player1Id, card)).error shouldNotBe null
            }
        }
    }
}
