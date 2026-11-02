package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m19.cards.ChaosWand
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Chaos Wand (M19 #228) — the target opponent exiles from the top of their library until an
 * instant or sorcery; the Wand's controller may cast it free during resolution, and every exiled
 * card not cast goes to the bottom of *that opponent's* library in a random order.
 */
class ChaosWandScenarioTest : ScenarioTestBase() {

    private val ability = ChaosWand.activatedAbilities[0].id

    private fun TestGame.namesIn(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    private fun setup(library: List<String> = listOf("Forest", "Grizzly Bears", "Divination", "Island")): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Chaos Wand")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (card in library) builder = builder.withCardInLibrary(2, card)
        repeat(3) { builder = builder.withCardInLibrary(1, "Plains") }
        return builder.build()
    }

    private fun TestGame.activate() {
        val wand = findPermanent("Chaos Wand")!!
        execute(
            ActivateAbility(player1Id, wand, ability, targets = listOf(ChosenTarget.Player(player2Id)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        context("Chaos Wand") {
            test("you cast the opponent's first instant or sorcery free; the rest go to the bottom of their library") {
                val game = setup()
                val handBefore = game.handSize(1)

                game.activate()
                withClue("the free cast is offered while the ability resolves") {
                    game.answerYesNo(true).error shouldBe null
                }
                game.resolveStack()

                withClue("Divination resolved for the Wand's controller") { game.handSize(1) shouldBe handBefore + 2 }
                game.namesIn(game.state.getGraveyard(game.player2Id)) shouldBe listOf("Divination")
                game.state.getExile(game.player2Id) shouldBe emptyList()
                val library = game.namesIn(game.state.getLibrary(game.player2Id))
                library.first() shouldBe "Island"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Forest", "Grizzly Bears")
            }

            test("declining the cast puts the instant or sorcery on the bottom with the others") {
                val game = setup()
                val handBefore = game.handSize(1)

                game.activate()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.state.getExile(game.player2Id) shouldBe emptyList()
                val library = game.namesIn(game.state.getLibrary(game.player2Id))
                library.first() shouldBe "Island"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Forest", "Grizzly Bears", "Divination")
            }

            test("with no instant or sorcery, the whole library is exiled and put back with no cast offered") {
                val game = setup(library = listOf("Forest", "Grizzly Bears", "Island"))

                game.activate()

                game.hasPendingDecision() shouldBe false
                game.state.getExile(game.player2Id) shouldBe emptyList()
                game.namesIn(game.state.getLibrary(game.player2Id)) shouldContainExactlyInAnyOrder
                    listOf("Forest", "Grizzly Bears", "Island")
            }
        }
    }
}
