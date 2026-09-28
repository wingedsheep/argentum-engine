package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Hope's Beacon (MOM #134).
 *
 * - Whenever you cast an instant or sorcery spell, copy it. You may choose new targets for the
 *   copy. This ability triggers only once each turn.
 * - +1: Exile the top five cards of your library. Until the end of your next turn, you may cast an
 *   instant or sorcery spell from among those exiled cards.
 * - −X: Chandra deals X damage to each of up to two targets.
 */
class ChandraHopesBeaconScenarioTest : ScenarioTestBase() {
    init {
        val abilities = cardRegistry.getCard("Chandra, Hope's Beacon")!!.script.activatedAbilities
        val plusOneId = abilities.single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id
        val minusXId = abilities.single { it.cost is AbilityCost.LoyaltyX }.id

        fun base() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Chandra, Hope's Beacon")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        /** Resolve everything, pointing the copy at the opponent when it offers new targets. */
        fun TestGame.drain() {
            repeat(20) {
                when (val decision = getPendingDecision()) {
                    is YesNoDecision -> answerYesNo(false)
                    is ChooseTargetsDecision -> selectTargets(listOf(player2Id))
                    null -> if (state.stack.isNotEmpty()) resolveStack() else return
                    else -> error("unexpected decision $decision")
                }
            }
        }

        fun TestGame.exiled(name: String): List<EntityId> = state.getExile(player1Id).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        fun TestGame.canCast(cardId: EntityId): Boolean =
            getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

        test("casting an instant copies it, but only the first instant or sorcery each turn") {
            val game = base()
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.drain()
            withClue("Bolt and its copy both hit the opponent") { game.getLifeTotal(2) shouldBe 14 }

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.drain()
            withClue("The second instant this turn isn't copied") { game.getLifeTotal(2) shouldBe 11 }
        }

        test("+1: exactly one instant or sorcery among the exiled cards may be cast") {
            val game = base()
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()
            val chandra = game.findPermanent("Chandra, Hope's Beacon")!!

            game.execute(ActivateAbility(game.player1Id, chandra, plusOneId)).error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 0
            val bolts = game.exiled("Lightning Bolt")
            bolts.size shouldBe 2
            withClue("Creatures and lands among the exiled cards aren't castable") {
                game.canCast(game.exiled("Grizzly Bears").single()) shouldBe false
                val mountains = game.exiled("Mountain")
                game.getLegalActions(1).none { (it.action as? PlayLand)?.cardId in mountains } shouldBe true
            }
            withClue("Either exiled instant is offered") {
                game.canCast(bolts[0]) shouldBe true
                game.canCast(bolts[1]) shouldBe true
            }

            game.execute(
                CastSpell(game.player1Id, bolts[0], targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.drain()
            withClue("The cast Bolt was copied by Chandra's static") { game.getLifeTotal(2) shouldBe 14 }

            withClue("Having cast one, the other exiled Bolt can no longer be cast") {
                game.canCast(bolts[1]) shouldBe false
                game.execute(
                    CastSpell(game.player1Id, bolts[1], targets = listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldNotBe null
            }
            game.state.getEntity(chandra)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 6
        }

        test("−X deals X damage to each of two targets") {
            val game = base().withCardOnBattlefield(2, "Hill Giant").build()
            val chandra = game.findPermanent("Chandra, Hope's Beacon")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chandra, minusXId,
                    targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player2Id)),
                    xValue = 2
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)!!.get<DamageComponent>()!!.amount shouldBe 2
            game.getLifeTotal(2) shouldBe 18
            game.state.getEntity(chandra)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 3
        }

        test("−X may choose a single target") {
            val game = base().build()
            val chandra = game.findPermanent("Chandra, Hope's Beacon")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chandra, minusXId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    xValue = 4
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
        }
    }
}
