package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Derevi, Empyrial Tactician — the first printed card whose activated ability functions from the
 * command zone (`activateFromZone = Zone.COMMAND`). Proves the ability is offered there and not from
 * the hand, that it puts Derevi onto the battlefield without casting it, and that the enters
 * trigger then taps or untaps a target permanent.
 */
class DereviEmpyrialTacticianScenarioTest : ScenarioTestBase() {

    private val abilityId
        get() = cardRegistry.getCard("Derevi, Empyrial Tactician")!!.script.activatedAbilities[0].id

    private fun TestGame.commandAction() =
        getLegalActions(1).firstOrNull {
            it.action.let { a -> a is ActivateAbility && a.abilityId == abilityId }
        }

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.chooseMode(prefix: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(prefix) }
        check(index >= 0) { "$prefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
    }

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Derevi, Empyrial Tactician") {

            test("activates from the command zone, enters, and its enters trigger taps a permanent") {
                val game = base().withCardInCommandZone(1, "Derevi, Empyrial Tactician").build()
                val derevi = game.state.getZone(game.player1Id, Zone.COMMAND).first()
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("the command-zone enumerator offers the ability") {
                    game.commandAction() shouldNotBe null
                }

                val result = game.execute(ActivateAbility(game.player1Id, derevi, abilityId))
                withClue("activation is legal: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Derevi left the command zone for the battlefield") {
                    game.isOnBattlefield("Derevi, Empyrial Tactician") shouldBe true
                    game.state.getZone(game.player1Id, Zone.COMMAND).contains(derevi) shouldBe false
                }

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                game.chooseMode("Tap")
                game.resolveStack()

                game.isTapped(bears) shouldBe true
            }

            test("the ability is not offered while Derevi is in hand") {
                val game = base().withCardInHand(1, "Derevi, Empyrial Tactician").build()
                game.commandAction() shouldBe null
            }
        }
    }
}
