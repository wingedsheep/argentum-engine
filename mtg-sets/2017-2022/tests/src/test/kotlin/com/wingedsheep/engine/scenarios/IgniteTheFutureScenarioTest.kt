package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ignite the Future (C19 #27, reprinted in J22) — {3}{R} Sorcery, Flashback {7}{R}.
 *
 *   Exile the top three cards of your library. Until the end of your next turn, you may play
 *   those cards. If this spell was cast from a graveyard, you may play cards this way without
 *   paying their mana costs.
 *
 * The free waiver must last as long as the permission: through the opponent's turn and into the
 * caster's next turn, ending at that turn's cleanup.
 */
class IgniteTheFutureScenarioTest : ScenarioTestBase() {

    init {
        context("Ignite the Future") {

            test("cast from hand: the exiled cards are playable but not free") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ignite the Future")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .apply { repeat(6) { withCardInLibrary(1, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ignite the Future").error shouldBe null
                game.resolveStack()

                val exiled = game.state.getExile(game.player1Id)
                    .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                exiled shouldHaveSize 3
                withClue("a hand cast grants no free waiver") {
                    exiled.filter { game.state.getEntity(it)?.get<PlayWithoutPayingCostComponent>() != null }.shouldBeEmpty()
                }
                withClue("with every Mountain tapped, an exiled Bears can't be cast") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldNotBe null
                }
            }

            test("flashback: the exiled cards are free until the end of the caster's next turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Ignite the Future")
                    .withLandsOnBattlefield(1, "Mountain", 8)
                    .apply { repeat(8) { withCardInLibrary(1, "Grizzly Bears") } }
                    .apply { repeat(4) { withCardInLibrary(2, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Ignite the Future").error shouldBe null
                game.resolveStack()

                withClue("the flashback cast exiled itself and three library cards") {
                    game.isInExile(1, "Ignite the Future") shouldBe true
                }

                withClue("all eight Mountains paid the flashback, yet a Bears casts for free this turn") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                    game.resolveStack()
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                // Opponent's turn: its cleanup must not strip the waiver.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id

                val mountains = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Mountain"
                }
                withClue("on the caster's next turn the second Bears is still free — no Mountain taps") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                    game.resolveStack()
                    mountains.count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 0
                }

                // The caster's next turn ends: permission and waiver both expire.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                val leftover = game.state.getExile(game.player1Id).filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                leftover shouldHaveSize 1
                withClue("the waiver ended with the caster's next turn") {
                    game.state.getEntity(leftover.single())?.get<PlayWithoutPayingCostComponent>() shouldBe null
                    game.state.mayPlayPermissions.none { leftover.single() in it.cardIds } shouldBe true
                }
            }
        }
    }
}
