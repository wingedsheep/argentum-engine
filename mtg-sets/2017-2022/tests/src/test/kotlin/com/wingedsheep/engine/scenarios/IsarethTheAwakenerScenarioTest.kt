package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Isareth the Awakener: "Whenever Isareth attacks, you may pay {X}. When you do, return target
 * creature card with mana value X from your graveyard to the battlefield with a corpse counter on
 * it. If that creature would leave the battlefield, exile it instead of putting it anywhere else."
 *
 * Paying {0} is a payment, not a decline — it returns a mana value 0 creature card.
 */
class IsarethTheAwakenerScenarioTest : ScenarioTestBase() {

    init {
        fun board(): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Isareth the Awakener")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInGraveyard(1, "Memnite")        // MV 0
            .withCardInGraveyard(1, "Grizzly Bears")  // MV 2
            .withCardInGraveyard(1, "Hill Giant")     // MV 4
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.attackWithIsareth() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Isareth the Awakener" to 2)).error shouldBe null
            resolveStack()
            withClue("the trigger first asks whether to pay {X}") {
                (getPendingDecision() is YesNoDecision) shouldBe true
            }
        }

        fun TestGame.corpseCounters(name: String): Int =
            state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.CORPSE) ?: 0

        context("Isareth the Awakener") {
            test("paying X = 2 returns only a mana value 2 creature card, with a corpse counter") {
                val game = board()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.attackWithIsareth()
                game.answerYesNo(true).error shouldBe null
                (game.getPendingDecision() is ChooseNumberDecision) shouldBe true
                game.chooseNumber(2).error shouldBe null

                val targets = game.getPendingDecision()
                withClue("the reflexive trigger targets after X is paid: $targets") {
                    (targets is ChooseTargetsDecision) shouldBe true
                }
                targets as ChooseTargetsDecision
                targets.legalTargets[0]?.toSet() shouldBe setOf(bears)
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.corpseCounters("Grizzly Bears") shouldBe 1
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("paying X = 0 returns a mana value 0 creature, and it is exiled when it would die") {
                val game = board()
                val memnite = game.findCardsInGraveyard(1, "Memnite").single()

                game.attackWithIsareth()
                game.answerYesNo(true).error shouldBe null
                game.chooseNumber(0).error shouldBe null

                val targets = game.getPendingDecision()
                withClue("paying {0} still fires the reflexive trigger: $targets") {
                    (targets is ChooseTargetsDecision) shouldBe true
                }
                targets as ChooseTargetsDecision
                targets.legalTargets[0]?.toSet() shouldBe setOf(memnite)
                game.selectTargets(listOf(memnite)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Memnite") shouldBe true
                game.corpseCounters("Memnite") shouldBe 1

                // Combat damage doesn't touch Memnite; kill it with a -1/-1 effect instead.
                val returned = game.findPermanent("Memnite")!!
                game.state = game.state.updateEntity(returned) { container ->
                    container.with(CountersComponent(mapOf(CounterType.CORPSE to 1, CounterType.MINUS_ONE_MINUS_ONE to 1)))
                }
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the returned creature is exiled instead of going to the graveyard") {
                    game.isInExile(1, "Memnite") shouldBe true
                    game.isInGraveyard(1, "Memnite") shouldBe false
                }
            }

            test("declining returns nothing") {
                val game = board()

                game.attackWithIsareth()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Memnite") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
