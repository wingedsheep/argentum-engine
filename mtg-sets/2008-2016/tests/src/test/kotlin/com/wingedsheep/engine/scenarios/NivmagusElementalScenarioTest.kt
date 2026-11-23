package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rtr.cards.NivmagusElemental
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nivmagus Elemental (RTR #219) — {U/R} Creature — Elemental 1/2.
 *
 *   Exile an instant or sorcery spell you control: Put two +1/+1 counters on this creature.
 *   (That spell won't resolve.)
 */
class NivmagusElementalScenarioTest : ScenarioTestBase() {

    private val abilityId = NivmagusElemental.activatedAbilities.first().id

    private fun TestGame.spellOnStack(name: String) =
        state.stack.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.elementalActions(elemental: com.wingedsheep.sdk.model.EntityId) =
        getLegalActions(1).filter { (it.action as? ActivateAbility)?.sourceId == elemental }

    init {
        test("exiling your own sorcery from the stack grows the Elemental and the sorcery never resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Nivmagus Elemental")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Divination")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elemental = game.findPermanent("Nivmagus Elemental")!!

            // Nothing of ours on the stack: the cost can't be paid.
            game.elementalActions(elemental).shouldBeEmpty()

            game.castSpell(1, "Divination").error shouldBe null
            val divination = game.spellOnStack("Divination")
            val offer = game.elementalActions(elemental).single()
            offer.additionalCostInfo!!.validExileTargets shouldContainExactly listOf(divination)
            // Our own spell on top is the window auto-pass skips; the ability holds priority there.
            offer.holdPriority shouldBe true

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = elemental,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(divination)),
                )
            ).error shouldBe null
            game.isInExile(1, "Divination") shouldBe true
            game.state.stack.size shouldBe 1 // only the ability

            game.resolveStack()
            game.state.projectedState.getPower(elemental) shouldBe 3
            game.state.projectedState.getToughness(elemental) shouldBe 4
            // Divination was exiled, not resolved: no cards drawn.
            game.state.getHand(game.player1Id).size shouldBe 0
        }

        test("creature spells and an opponent's spells can't pay the cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Nivmagus Elemental")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(2, "Opt")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elemental = game.findPermanent("Nivmagus Elemental")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Opt").error shouldBe null
            game.passPriority().error shouldBe null
            game.state.priorityPlayerId shouldBe game.player1Id

            // Grizzly Bears is a creature spell and Opt is the opponent's: neither pays.
            game.elementalActions(elemental).shouldBeEmpty()

            // A client-supplied payment naming the opponent's instant is rejected.
            val opt = game.spellOnStack("Opt")
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = elemental,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(opt)),
                )
            ).error shouldNotBe null
            game.state.stack.contains(opt) shouldBe true
        }
    }
}
