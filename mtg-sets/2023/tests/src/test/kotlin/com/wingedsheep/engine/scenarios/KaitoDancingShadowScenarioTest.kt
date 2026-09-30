package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kaito, Dancing Shadow (ONE #204) — "Whenever one or more creatures you control deal combat damage
 * to a player, you may return one of them to its owner's hand. If you do, you may activate loyalty
 * abilities of Kaito twice this turn rather than only once."
 *
 * Proves the trigger offers exactly the creatures that hit, returning one lets Kaito activate twice
 * that turn (per the ruling, the same ability twice is fine), declining leaves him at once, and the
 * −2 makes the deathtouch Drone that drains on leaving.
 */
class KaitoDancingShadowScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Kaito, Dancing Shadow")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.kaitoAbility(change: Int) =
        cardRegistry.getCard("Kaito, Dancing Shadow")!!.script.activatedAbilities
            .first { (it.cost as? AbilityCost.Loyalty)?.change == change }

    private fun TestGame.activateKaito(change: Int) =
        execute(ActivateAbility(player1Id, findPermanent("Kaito, Dancing Shadow")!!, kaitoAbility(change).id))

    private fun TestGame.kaitoOffered(): Boolean {
        val kaito = findPermanent("Kaito, Dancing Shadow")!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == kaito }
    }

    private fun TestGame.names(ids: List<EntityId>) = ids.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    /** Attack with Bears and Giant, reach the trigger's choice, answer it with [returnName] (or decline). */
    private fun TestGame.attackAndAnswer(returnName: String?): List<String> {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision == null && guard++ < 10) passPriority()
        val decision = state.pendingDecision as? SelectCardsDecision
        decision.shouldNotBeNull()
        val offered = names(decision.options)
        val pick = decision.options.filter { returnName != null && names(listOf(it)).single() == returnName }
        selectCards(pick).error shouldBe null
        resolveStack()
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        return offered
    }

    init {
        test("returning one of the creatures that hit lets Kaito activate twice this turn") {
            val game = board()
            val offered = game.attackAndAnswer(returnName = "Grizzly Bears")

            withClue("only the creatures that dealt combat damage are offered") {
                offered shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Hill Giant")
            }
            game.getLifeTotal(2) shouldBe 15
            game.isInHand(1, "Grizzly Bears") shouldBe true

            val handBefore = game.handSize(1)
            game.activateKaito(0).error shouldBe null
            game.resolveStack()
            withClue("a second activation — even of the same ability — is allowed") {
                game.kaitoOffered() shouldBe true
                game.activateKaito(0).error shouldBe null
            }
            game.resolveStack()
            game.handSize(1) shouldBe handBefore + 2
            withClue("but not a third") {
                game.kaitoOffered() shouldBe false
                game.activateKaito(0).error shouldNotBe null
            }
        }

        test("declining the return leaves Kaito at one activation") {
            val game = board()
            game.attackAndAnswer(returnName = null)
            game.isOnBattlefield("Grizzly Bears") shouldBe true

            game.activateKaito(0).error shouldBe null
            game.resolveStack()
            game.kaitoOffered() shouldBe false
            game.activateKaito(0).error shouldNotBe null
        }

        test("−2 creates a deathtouch Drone that drains each opponent for 2 when it leaves") {
            val game = board()
            game.activateKaito(-2).error shouldBe null
            game.resolveStack()

            val drone = game.findPermanent("Drone Token") ?: game.findPermanent("Drone")
            drone.shouldNotBeNull()
            game.state.projectedState.hasKeyword(drone, Keyword.DEATHTOUCH) shouldBe true
            game.state.projectedState.getPower(drone) shouldBe 2
            game.state.projectedState.getToughness(drone) shouldBe 2

            game.castSpell(1, "Unsummon", targetId = drone).error shouldBe null
            game.resolveStack()
            withClue("the Drone's leaves trigger drains each opponent for 2") {
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }
        }
    }
}
