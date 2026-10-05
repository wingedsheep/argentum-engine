package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Djinn of Wishes (M10 #50) — {3}{U}{U} Creature — Djinn 4/4.
 *
 *   Flying
 *   This creature enters with three wish counters on it.
 *   {2}{U}{U}, Remove a wish counter from this creature: Reveal the top card of your library. You
 *   may play that card without paying its mana cost. If you don't, exile it.
 *
 * Covers the play during resolution for both halves of "play": a spell is cast for free, and a
 * land is played straight from the library using up the turn's land play (CR 305.2a). A land that
 * can't be played — the land play is gone (CR 305.2b) or it isn't your turn (CR 305.3) — is
 * exiled, as is a declined card. Nothing is left playable once the ability has resolved.
 */
class DjinnOfWishesScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy { cardRegistry.getCard("Djinn of Wishes")!!.script.activatedAbilities[0].id }

    private fun TestGame.castDjinn(): EntityId {
        castSpell(1, "Djinn of Wishes").error shouldBe null
        resolveStack()
        return findPermanent("Djinn of Wishes")!!
    }

    private fun TestGame.wishCounters(djinn: EntityId): Int =
        state.getEntity(djinn)?.get<CountersComponent>()?.getCount(CounterType.WISH) ?: 0

    private fun TestGame.activateDjinn(djinn: EntityId) {
        execute(ActivateAbility(playerId = player1Id, sourceId = djinn, abilityId = abilityId)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.permanentsNamed(name: String): Int =
        state.getBattlefield(player1Id).count { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun djinnGame(top: String, extraHand: String? = null) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Djinn of Wishes")
        .apply { if (extraHand != null) withCardInHand(1, extraHand) }
        .withLandsOnBattlefield(1, "Island", 9)
        .withCardInLibrary(1, top)
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Djinn of Wishes") {

            test("enters with three wish counters; activating spends one") {
                val game = djinnGame(top = "Grizzly Bears")
                val djinn = game.castDjinn()
                game.wishCounters(djinn) shouldBe 3

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = djinn, abilityId = abilityId))
                    .error shouldBe null
                game.wishCounters(djinn) shouldBe 2
            }

            test("a revealed spell is cast without paying its mana cost") {
                val game = djinnGame(top = "Grizzly Bears")
                val djinn = game.castDjinn()
                game.activateDjinn(djinn)

                withClue("the controller is asked whether to play the revealed card") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Grizzly Bears was cast for free and resolved; nothing was exiled") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    game.state.getExile(game.player1Id) shouldBe emptyList()
                    game.librarySize(1) shouldBe 1
                }
            }

            test("a revealed land is played from the library and uses up the land play") {
                val game = djinnGame(top = "Forest", extraHand = "Plains")
                val djinn = game.castDjinn()
                game.activateDjinn(djinn)
                game.answerYesNo(true)

                withClue("the Forest moved from the library onto the battlefield") {
                    game.permanentsNamed("Forest") shouldBe 1
                    game.librarySize(1) shouldBe 1
                    game.state.getExile(game.player1Id) shouldBe emptyList()
                }
                withClue("no play permission survives the ability's resolution") {
                    game.state.mayPlayPermissions.flatMap { it.cardIds } shouldBe emptyList()
                }

                val plains = game.findCardsInHand(1, "Plains").single()
                withClue("playing the Forest was this turn's land play (CR 305.2a)") {
                    game.execute(PlayLand(game.player1Id, plains)).error shouldNotBe null
                }
            }

            test("a revealed land is exiled when the land play was already used") {
                val game = djinnGame(top = "Forest", extraHand = "Plains")
                val plains = game.findCardsInHand(1, "Plains").single()
                game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
                val djinn = game.castDjinn()
                game.activateDjinn(djinn)
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(true)

                withClue("the Forest can't be played (CR 305.2b), so it is exiled") {
                    game.permanentsNamed("Forest") shouldBe 0
                    game.state.getExile(game.player1Id).size shouldBe 1
                    game.librarySize(1) shouldBe 1
                }
            }

            test("a revealed land is exiled when the ability resolves on an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Djinn of Wishes")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val djinn = game.findPermanent("Djinn of Wishes")!!
                game.state = game.state.updateEntity(djinn) {
                    it.with(CountersComponent(mapOf(CounterType.WISH to 1)))
                }
                game.activateDjinn(djinn)
                if (game.getPendingDecision() is YesNoDecision) game.answerYesNo(true)

                withClue("a land can't be played on another player's turn (CR 305.3), so it is exiled") {
                    game.permanentsNamed("Forest") shouldBe 0
                    game.state.getExile(game.player1Id).size shouldBe 1
                }
            }

            test("declining exiles the revealed card") {
                val game = djinnGame(top = "Grizzly Bears")
                val djinn = game.castDjinn()
                game.activateDjinn(djinn)
                game.answerYesNo(false)

                withClue("the declined Grizzly Bears is exiled, not left on top of the library") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.state.getExile(game.player1Id).size shouldBe 1
                    game.librarySize(1) shouldBe 1
                }
            }
        }
    }
}
