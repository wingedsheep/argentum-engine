package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pyxis of Pandemonium (THS #220) — {1} Artifact.
 *
 *   {T}: Each player exiles the top card of their library face down.
 *   {7}, {T}, Sacrifice this artifact: Each player turns face up all cards they own exiled with
 *   this artifact, then puts all permanent cards among them onto the battlefield.
 *
 * Pins the turn-face-up-in-exile step: the pile goes face down, comes face up in place, its
 * permanent cards enter under their owners' control and its non-permanent cards stay in exile,
 * face up. A second Pyxis can't reach the first one's pile.
 */
class PyxisOfPandemoniumScenarioTest : ScenarioTestBase() {

    private val tapAbility get() = cardRegistry.getCard("Pyxis of Pandemonium")!!.activatedAbilities[0].id
    private val sacAbility get() = cardRegistry.getCard("Pyxis of Pandemonium")!!.activatedAbilities[1].id

    private fun board(pyxisCount: Int = 1) = scenario()
        .withPlayers("Alice", "Bob")
        .apply { repeat(pyxisCount) { withCardOnBattlefield(1, "Pyxis of Pandemonium") } }
        .withLandsOnBattlefield(1, "Swamp", 7)
        // Index 0 is the top of the library.
        .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(2, "Hill Giant").withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate(pyxis: EntityId, ability: AbilityId) {
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = pyxis, abilityId = ability))
        withClue("${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    private fun TestGame.untap(pyxis: EntityId) {
        state = state.updateEntity(pyxis) { it.without<TappedComponent>() }
    }

    private fun TestGame.exiled(player: Int, name: String): EntityId =
        state.getExile(if (player == 1) player1Id else player2Id)
            .single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.faceDown(id: EntityId) = state.getEntity(id)?.has<FaceDownComponent>() == true

    init {
        test("the tap exiles each player's top card face down; the sacrifice turns them up and lands the permanents") {
            val game = board()
            val pyxis = game.findPermanent("Pyxis of Pandemonium")!!

            game.activate(pyxis, tapAbility)
            game.untap(pyxis)
            game.activate(pyxis, tapAbility)

            withClue("two cards from each library, all face down") {
                game.librarySize(1) shouldBe 0
                game.librarySize(2) shouldBe 0
                game.faceDown(game.exiled(1, "Grizzly Bears")) shouldBe true
                game.faceDown(game.exiled(1, "Lightning Bolt")) shouldBe true
                game.faceDown(game.exiled(2, "Hill Giant")) shouldBe true
                game.faceDown(game.exiled(2, "Mountain")) shouldBe true
            }

            game.untap(pyxis)
            game.activate(pyxis, sacAbility)

            game.isInGraveyard(1, "Pyxis of Pandemonium") shouldBe true
            withClue("each permanent card enters under its owner's control") {
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id
                game.state.projectedState.getController(game.findPermanent("Hill Giant")!!) shouldBe game.player2Id
                game.state.projectedState.getController(game.findPermanent("Mountain")!!) shouldBe game.player2Id
            }
            withClue("the instant stays in exile, now face up") {
                game.faceDown(game.exiled(1, "Lightning Bolt")) shouldBe false
            }
        }

        test("a second Pyxis can't turn up or return the first one's pile") {
            val game = board(pyxisCount = 2)
            val (first, second) = game.findPermanents("Pyxis of Pandemonium")

            game.activate(first, tapAbility)
            game.activate(second, sacAbility)

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.faceDown(game.exiled(1, "Grizzly Bears")) shouldBe true
            game.faceDown(game.exiled(2, "Hill Giant")) shouldBe true
        }
    }
}
