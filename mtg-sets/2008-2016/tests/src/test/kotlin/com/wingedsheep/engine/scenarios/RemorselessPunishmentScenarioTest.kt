package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Remorseless Punishment (OGW #89) — {3}{B}{B} Sorcery: "Target opponent loses 5 life unless that
 * player discards two cards or sacrifices a creature or planeswalker of their choice. Repeat this
 * process once."
 *
 * The targeted opponent is asked twice, choosing afresh each time, and an option they can no
 * longer take drops out: discarding needs at least two cards in hand (Scryfall ruling), so after
 * discarding their last two cards only the sacrifice and the life loss remain.
 */
class RemorselessPunishmentScenarioTest : ScenarioTestBase() {

    init {
        context("Remorseless Punishment") {

            test("the opponent discards two, then — with an empty hand — sacrifices a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Remorseless Punishment")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Remorseless Punishment", 2).error shouldBe null
                game.resolveStack()

                val first = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the targeted opponent makes the choice") { first.playerId shouldBe game.player2Id }
                withClue("discard two, sacrifice, or lose 5 life") { first.options.size shouldBe 3 }
                game.submitDecision(OptionChosenResponse(first.id, 0))
                val discard = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(discard.options)

                val second = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the same opponent is asked again") { second.playerId shouldBe game.player2Id }
                withClue("an empty hand can't discard two: sacrifice or lose 5 life") {
                    second.options.size shouldBe 2
                }
                game.submitDecision(OptionChosenResponse(second.id, 0))
                val sacrifice = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                sacrifice.options shouldBe listOf(game.findPermanent("Grizzly Bears")!!)
                game.selectCards(sacrifice.options)

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 20
                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("one card in hand and no creature or planeswalker: the opponent loses 5 life twice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Remorseless Punishment")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Remorseless Punishment", 2).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 10
                game.handSize(2) shouldBe 1
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
