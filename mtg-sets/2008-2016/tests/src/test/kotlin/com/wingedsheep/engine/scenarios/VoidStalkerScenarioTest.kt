package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m13.cards.VoidStalker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Void Stalker — "{2}{U}, {T}: Put this creature and target creature on top of their owners'
 * libraries, then those players shuffle their libraries."
 *
 * Ruling: if Void Stalker leaves the battlefield before the ability resolves, the target is still
 * shuffled away but Void Stalker stays wherever it is.
 */
class VoidStalkerScenarioTest : ScenarioTestBase() {

    private val abilityId = VoidStalker.activatedAbilities.first().id

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Void Stalker")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInHand(1, "Unsummon")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.activateOn(cardName: String) {
        val stalker = findPermanent("Void Stalker")!!
        val target = findPermanent(cardName)!!
        val result = execute(
            ActivateAbility(player1Id, stalker, abilityId, listOf(ChosenTarget.Permanent(target)))
        )
        withClue("${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        test("puts itself and the target creature into their owners' libraries") {
            val game = board()
            game.activateOn("Grizzly Bears")
            game.resolveStack()

            game.findPermanent("Void Stalker") shouldBe null
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findCardsInLibrary(1, "Void Stalker") shouldHaveSize 1
            game.findCardsInLibrary(2, "Grizzly Bears") shouldHaveSize 1
        }

        test("if Void Stalker leaves before resolution, only the target goes to its library") {
            val game = board()
            game.activateOn("Grizzly Bears")
            val stalker = game.findPermanent("Void Stalker")!!
            game.castSpell(1, "Unsummon", stalker).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.findCardsInHand(1, "Void Stalker") shouldHaveSize 1
            game.findCardsInLibrary(1, "Void Stalker") shouldHaveSize 0
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findCardsInLibrary(2, "Grizzly Bears") shouldHaveSize 1
        }

        test("targeting itself puts it into its owner's library once") {
            val game = board()
            game.activateOn("Void Stalker")
            game.resolveStack()

            game.findPermanent("Void Stalker") shouldBe null
            game.findCardsInLibrary(1, "Void Stalker") shouldHaveSize 1
            game.findCardsInLibrary(2, "Grizzly Bears") shouldHaveSize 0
        }
    }
}
