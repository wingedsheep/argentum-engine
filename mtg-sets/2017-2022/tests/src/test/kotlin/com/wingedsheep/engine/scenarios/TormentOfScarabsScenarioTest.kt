package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Torment of Scarabs (HOU #78) — {3}{B} Enchantment — Aura Curse: "Enchant player / At the
 * beginning of enchanted player's upkeep, that player loses 3 life unless they sacrifice a nonland
 * permanent of their choice or discard a card."
 *
 * The enchanted player — not the Curse's controller — is asked, pays, and loses the life.
 */
class TormentOfScarabsScenarioTest : ScenarioTestBase() {

    /** Player 1 curses player 2, then passes into player 2's upkeep, stopping at the trigger's choice. */
    private fun cursedOpponent(withBears: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Torment of Scarabs")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withLandsOnBattlefield(2, "Swamp", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withBears) builder.withCardOnBattlefield(2, "Grizzly Bears")
        repeat(3) {
            builder.withCardInLibrary(1, "Swamp")
            builder.withCardInLibrary(2, "Swamp")
        }
        val game = builder.build()

        game.castSpellTargetingPlayer(1, "Torment of Scarabs", 2).error shouldBe null
        game.resolveStack()
        val curse = game.findPermanent("Torment of Scarabs")!!
        game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.player2Id

        var guard = 0
        while (game.state.pendingDecision == null && guard++ < 200 &&
            !(game.state.activePlayerId == game.player2Id && game.state.step == Step.DRAW)
        ) {
            game.execute(PassPriority(game.state.priorityPlayerId ?: error("no priority at ${game.state.step}")))
        }
        return game
    }

    init {
        context("Torment of Scarabs") {

            test("the enchanted player sacrifices a nonland permanent to keep their life") {
                val game = cursedOpponent(withBears = true)

                val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("asked of the enchanted player in their upkeep") {
                    choice.playerId shouldBe game.player2Id
                    game.state.step shouldBe Step.UPKEEP
                }
                withClue("empty hand: sacrifice or lose life") { choice.options.size shouldBe 2 }
                game.submitDecision(OptionChosenResponse(choice.id, 0))
                val pick = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(pick.options)

                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("declining costs the enchanted player 3 life, not the Curse's controller") {
                val game = cursedOpponent(withBears = true)

                val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(choice.id, choice.options.lastIndex))

                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("with only lands and an empty hand, the enchanted player just loses 3 life") {
                val game = cursedOpponent(withBears = false)

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
