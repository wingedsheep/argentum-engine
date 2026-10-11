package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Intellectual Offering (C14 #15) — {4}{U} Instant
 * "Choose an opponent. You and that player each draw three cards.
 *  Choose an opponent. Untap all nonland permanents you control and all nonland permanents that
 *  player controls."
 *
 * Proves the chosen opponent is honoured by both halves: the draw reaches them through
 * `Player.ChosenOpponent`, and the untap reaches *their* nonland permanents through a
 * `ForEachPlayer(ChosenOpponent)` that rebinds "you control" — while lands stay tapped.
 */
class IntellectualOfferingScenarioTest : ScenarioTestBase() {

    init {
        test("both players draw three; both players' nonland permanents untap, lands stay tapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Intellectual Offering")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true, summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant", tapped = true, summoningSickness = false)
                .withCardOnBattlefield(2, "Mountain", tapped = true)
                .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Mountain") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spellId = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Intellectual Offering"
            }
            val cast = game.execute(CastSpell(game.player1Id, spellId, emptyList()))
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            fun tapped(player: Int, name: String): Boolean {
                val pid = if (player == 1) game.player1Id else game.player2Id
                val id = game.state.getBattlefield(pid).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }
                return game.state.getEntity(id)?.has<TappedComponent>() == true
            }

            withClue("you and the chosen opponent each drew three") {
                game.state.getHand(game.player1Id).size shouldBe 3
                game.state.getHand(game.player2Id).size shouldBe 3
            }
            withClue("your nonland permanent untapped") { tapped(1, "Grizzly Bears") shouldBe false }
            withClue("the chosen opponent's nonland permanent untapped") { tapped(2, "Hill Giant") shouldBe false }
            withClue("lands are not untapped") {
                tapped(1, "Island") shouldBe true
                tapped(2, "Mountain") shouldBe true
            }
        }
    }
}
