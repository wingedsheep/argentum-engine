package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.ProfaneProcession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Profane Procession // Tomb of the Dusk Rose (RIX #166).
 *
 * Pins the linked-exile count read on resolution (the third linked card transforms it; an
 * unrelated exiled card doesn't count) and the Tomb returning a creature card from that linked
 * pile under its activator's control, even one an opponent owns.
 */
class ProfaneProcessionScenarioTest : ScenarioTestBase() {

    private val exileAbilityId get() = ProfaneProcession.activatedAbilities[0].id
    private val reanimateAbilityId get() = ProfaneProcession.backFace!!.activatedAbilities[1].id

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Profane Procession")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInExile(2, "Grizzly Bears")
        .withCardInExile(2, "Wind Drake")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withLandsOnBattlefield(1, "Swamp", 5)
        // 5 + 5 lands: {3}{W}{B} to exile, then {2}{W}{B} for the Tomb, whichever lands autopay taps first
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.exiled(cardName: String): EntityId =
        state.getExile(player2Id).single { name(it) == cardName }

    private fun TestGame.link(procession: EntityId, cards: List<EntityId>) {
        state = state.updateEntity(procession) { it.with(LinkedExileComponent(cards)) }
    }

    private fun TestGame.exileGiant(procession: EntityId) {
        val giant = findPermanent("Hill Giant")!!
        val result = execute(
            ActivateAbility(player1Id, procession, exileAbilityId, targets = listOf(ChosenTarget.Permanent(giant)))
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        test("exiling the third linked card transforms it into Tomb of the Dusk Rose") {
            val game = game()
            val procession = game.findPermanent("Profane Procession")!!
            game.link(procession, listOf(game.exiled("Grizzly Bears"), game.exiled("Wind Drake")))

            game.exileGiant(procession)

            game.findPermanent("Hill Giant") shouldBe null
            game.name(procession) shouldBe "Tomb of the Dusk Rose"
        }

        test("cards exiled by something else don't count toward the three") {
            val game = game()
            val procession = game.findPermanent("Profane Procession")!!
            game.link(procession, listOf(game.exiled("Grizzly Bears")))

            game.exileGiant(procession)

            withClue("two linked cards plus an unrelated exiled Wind Drake") {
                game.name(procession) shouldBe "Profane Procession"
            }
        }

        test("Tomb puts a linked creature card onto the battlefield under your control") {
            val game = game()
            val procession = game.findPermanent("Profane Procession")!!
            game.link(procession, listOf(game.exiled("Grizzly Bears"), game.exiled("Wind Drake")))
            game.exileGiant(procession)
            val giant = game.exiled("Hill Giant")
            game.state.getEntity(procession)!!.get<LinkedExileComponent>()!!.exiledIds shouldContain giant

            game.execute(ActivateAbility(game.player1Id, procession, reanimateAbilityId)).error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getController(returned) shouldBe game.player1Id
        }
    }
}
