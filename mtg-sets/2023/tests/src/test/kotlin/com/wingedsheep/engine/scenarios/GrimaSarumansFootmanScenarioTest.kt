package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gríma, Saruman's Footman (LTC #57) — can't be blocked; "Whenever Gríma deals combat damage to a
 * player, that player exiles cards from the top of their library until they exile an instant or
 * sorcery card. You may cast that card without paying its mana cost. Then that player puts the
 * exiled cards that weren't cast this way on the bottom of their library in a random order."
 *
 * Chaos Wand's pipeline walked over the library of the player Gríma hit rather than a targeted
 * opponent, so these pin that the right library is read and refilled and that the cast is yours.
 */
class GrimaSarumansFootmanScenarioTest : ScenarioTestBase() {

    private fun setup(opponentLibrary: List<String> = listOf("Forest", "Grizzly Bears", "Divination", "Island")): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Gríma, Saruman's Footman")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (card in opponentLibrary) builder = builder.withCardInLibrary(2, card)
        repeat(3) { builder = builder.withCardInLibrary(1, "Plains") }
        return builder.build()
    }

    private fun TestGame.namesIn(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    private fun TestGame.hit() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Gríma, Saruman's Footman" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
    }

    init {
        context("Gríma, Saruman's Footman") {
            test("can't be blocked") {
                val game = setup()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Gríma, Saruman's Footman" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the opposing Grizzly Bears can't block it") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Gríma, Saruman's Footman"))).error shouldNotBe null
                }
            }

            test("you cast the opponent's first instant or sorcery free; the rest go to the bottom of their library") {
                val game = setup()
                val handBefore = game.handSize(1)

                game.hit()
                withClue("the free cast is offered while the ability resolves") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Divination resolved for Gríma's controller, not the opponent") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                game.namesIn(game.state.getGraveyard(game.player2Id)) shouldBe listOf("Divination")
                game.state.getExile(game.player2Id) shouldBe emptyList()
                val library = game.namesIn(game.state.getLibrary(game.player2Id))
                library.first() shouldBe "Island"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Forest", "Grizzly Bears")
            }

            test("declining the cast puts the instant or sorcery on the bottom with the others") {
                val game = setup()
                val handBefore = game.handSize(1)

                game.hit()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.state.getExile(game.player2Id) shouldBe emptyList()
                val library = game.namesIn(game.state.getLibrary(game.player2Id))
                library.first() shouldBe "Island"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Forest", "Grizzly Bears", "Divination")
            }

            test("with no instant or sorcery in their library nothing can be cast and all of it goes back") {
                val game = setup(opponentLibrary = listOf("Forest", "Grizzly Bears", "Island"))

                game.hit()

                game.hasPendingDecision() shouldBe false
                game.state.getExile(game.player2Id) shouldBe emptyList()
                game.namesIn(game.state.getLibrary(game.player2Id)) shouldContainExactlyInAnyOrder
                    listOf("Forest", "Grizzly Bears", "Island")
            }

            test("Gríma's controller's own library is never touched") {
                val game = setup()
                val myLibrary = game.librarySize(1)

                game.hit()
                game.answerYesNo(false)
                game.resolveStack()

                game.librarySize(1) shouldBe myLibrary
            }
        }
    }
}
