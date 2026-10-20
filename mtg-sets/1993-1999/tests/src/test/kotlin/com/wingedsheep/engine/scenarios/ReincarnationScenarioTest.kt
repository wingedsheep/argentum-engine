package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reincarnation (LEG #201) — "Choose target creature. When that creature dies this turn, return a
 * creature card from its owner's graveyard to the battlefield under the control of that creature's
 * owner."
 *
 * Rulings: Reincarnation's controller chooses the card when the delayed trigger resolves; it comes
 * from, and enters under the control of, the dead creature's *owner* — not its controller — and the
 * dead creature itself may be chosen.
 */
class ReincarnationScenarioTest : ScenarioTestBase() {

    init {
        context("Reincarnation") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withCardInHand(1, "Reincarnation")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Caught Red-Handed")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Forest")
                    builder.withCardInLibrary(2, "Mountain")
                }
                return builder.build()
            }

            fun TestGame.reincarnateAndBolt() {
                val bears = findPermanent("Grizzly Bears")!!
                castSpell(1, "Reincarnation", bears).error shouldBe null
                resolveStack()
                castSpell(1, "Lightning Bolt", bears).error shouldBe null
                resolveStack()
            }

            test("the caster returns a creature card from the dead creature's owner's graveyard under that owner's control") {
                val game = setup()
                game.reincarnateAndBolt()

                val choice = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses, from the opponent's graveyard only") {
                    choice.playerId shouldBe game.player1Id
                    choice.options shouldContainExactlyInAnyOrder
                        game.findCardsInGraveyard(2, "Grizzly Bears") + game.findCardsInGraveyard(2, "Hill Giant")
                }
                game.selectCards(game.findCardsInGraveyard(2, "Hill Giant")).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getController(giant) shouldBe game.player2Id
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            }

            test("the dead creature itself may come back, as a new object under its owner") {
                val game = setup()
                game.reincarnateAndBolt()
                game.selectCards(game.findCardsInGraveyard(2, "Grizzly Bears")).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                game.state.projectedState.getController(bears!!) shouldBe game.player2Id
            }

            test("a stolen creature reads its owner's graveyard and returns the card to its owner") {
                val game = setup()
                game.castSpell(1, "Caught Red-Handed", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id

                game.reincarnateAndBolt()
                val choice = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the owner's graveyard, not the stealing caster's") {
                    choice.options shouldContainExactlyInAnyOrder
                        game.findCardsInGraveyard(2, "Grizzly Bears") + game.findCardsInGraveyard(2, "Hill Giant")
                }
                game.selectCards(game.findCardsInGraveyard(2, "Hill Giant")).error shouldBe null
                game.state.projectedState.getController(game.findPermanent("Hill Giant")!!) shouldBe game.player2Id
            }
        }
    }
}
