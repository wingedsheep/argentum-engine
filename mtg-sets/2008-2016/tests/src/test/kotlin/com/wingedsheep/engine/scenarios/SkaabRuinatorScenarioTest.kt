package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skaab Ruinator — "As an additional cost to cast this spell, exile three creature cards from your
 * graveyard." plus "You may cast this card from your graveyard." The card is on the stack while its
 * costs are paid, so when cast from the graveyard it can't be one of the three it exiles.
 */
class SkaabRuinatorScenarioTest : ScenarioTestBase() {
    init {
        test("cast from the graveyard, exiling three other creature cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Skaab Ruinator")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ruinator = game.findCardsInGraveyard(1, "Skaab Ruinator").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears")

            val offer = game.getLegalActions(1).singleOrNull { (it.action as? CastSpell)?.cardId == ruinator }
            withClue("casting from the graveyard is offered") { offer shouldNotBe null }
            withClue("the exile pool holds the other creature cards, never Skaab Ruinator itself") {
                offer!!.additionalCostInfo!!.validExileTargets shouldContainExactlyInAnyOrder bears
            }

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = ruinator,
                    targets = emptyList(),
                    additionalCostPayment = AdditionalCostPayment(exiledCards = bears),
                ),
            )
            withClue("casting should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Skaab Ruinator") shouldBe true
            game.findCardsInGraveyard(1, "Grizzly Bears").shouldBeEmpty()
            game.isInExile(1, "Grizzly Bears") shouldBe true
        }

        test("Skaab Ruinator can't count itself among the three creature cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Skaab Ruinator")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ruinator = game.findCardsInGraveyard(1, "Skaab Ruinator").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears")

            withClue("only two other creature cards: the cast is not affordable") {
                game.getLegalActions(1).none { it.isAffordable && (it.action as? CastSpell)?.cardId == ruinator } shouldBe true
            }

            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = ruinator,
                    targets = emptyList(),
                    additionalCostPayment = AdditionalCostPayment(exiledCards = bears + ruinator),
                ),
            )
            withClue("a payment naming Skaab Ruinator itself is rejected") { cast.error shouldNotBe null }
            game.isInGraveyard(1, "Skaab Ruinator") shouldBe true
        }
    }
}
