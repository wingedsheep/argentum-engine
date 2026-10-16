package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.pip.cards.RaulTroubleShooter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Raul, Trouble Shooter — "Once during each of your turns, you may cast a spell from among cards
 * in your graveyard that were milled this turn. {T}: Each player mills a card."
 *
 * Proves the `MilledThisTurn` filter is read at graveyard-cast time (a card put there some other
 * way is never offered) and that the allowance is spent by the first cast.
 */
class RaulTroubleShooterScenarioTest : ScenarioTestBase() {
    private val millSelfTwo = card("Raul Mill Self Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.You) } }
    }

    private val abilityId = RaulTroubleShooter.activatedAbilities.first().id

    private fun board(library: List<String>, graveyard: List<String> = emptyList()): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Raul, Trouble Shooter")
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardInHand(1, millSelfTwo.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        library.forEach { b.withCardInLibrary(1, it) }
        repeat(3) { b.withCardInLibrary(2, "Forest") }
        graveyard.forEach { b.withCardInGraveyard(1, it) }
        return b.build()
    }

    init {
        cardRegistry.register(millSelfTwo)

        test("tapping mills each player, and a creature it milled can be cast from the graveyard") {
            val game = board(library = listOf("Grizzly Bears", "Grizzly Bears"))
            val raul = game.findPermanent("Raul, Trouble Shooter")!!
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = raul, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()
            game.graveyardSize(1) shouldBe 1
            game.graveyardSize(2) shouldBe 1

            game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a card that reached the graveyard without being milled is not offered") {
            val game = board(library = listOf("Forest", "Forest"), graveyard = listOf("Grizzly Bears"))
            game.castSpell(1, millSelfTwo.name).error shouldBe null
            game.resolveStack()
            game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldNotBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("only one milled spell can be cast this way each turn") {
            val game = board(library = listOf("Grizzly Bears", "Grizzly Bears"))
            game.castSpell(1, millSelfTwo.name).error shouldBe null
            game.resolveStack()
            game.graveyardSize(1) shouldBe 3 // two Bears plus the resolved instant

            game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.castSpellFromGraveyard(1, "Grizzly Bears").error shouldNotBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
