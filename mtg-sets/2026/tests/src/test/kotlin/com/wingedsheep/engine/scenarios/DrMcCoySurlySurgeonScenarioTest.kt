package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dr. McCoy, Surly Surgeon: the ETB puts a +1/+1 counter on another creature you control, and
 * "if another creature you control with a +1/+1 counter on it would be destroyed, instead remove
 * all +1/+1 counters from it and all damage already dealt to it is healed".
 */
class DrMcCoySurlySurgeonScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.givePlusOneCounter(id: EntityId) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }

    private fun board(vararg extra: Pair<Int, String>) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Dr. McCoy, Surly Surgeon")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .apply { extra.forEach { (p, name) -> withCardOnBattlefield(p, name) } }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Dr. McCoy, Surly Surgeon") {
            test("entering puts a +1/+1 counter on another target creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dr. McCoy, Surly Surgeon")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Dr. McCoy, Surly Surgeon").error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(bears)).error shouldBe null
                }
                game.resolveStack()

                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(game.findPermanent("Dr. McCoy, Surly Surgeon")!!) shouldBe 0
            }

            test("lethal damage: the counters come off and the damage is healed instead of dying") {
                val game = board().withCardInHand(1, "Lightning Bolt").withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2).build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.givePlusOneCounter(bears)

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("3 damage to a 3/3 is replaced, not lethal") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
                withClue("all +1/+1 counters removed") { game.plusOneCounters(bears) shouldBe 0 }
                withClue("all damage healed") { game.damageOn(bears) shouldBe 0 }

                // No counter left, so the next destruction isn't replaced.
                game.castSpell(1, "Shock", bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("a destroy effect is replaced too") {
                val game = board().withCardInHand(1, "Doom Blade").withLandsOnBattlefield(1, "Swamp", 2).build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.givePlusOneCounter(bears)

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.plusOneCounters(bears) shouldBe 0
            }

            test("a board wipe kills McCoy and creatures without counters, but the countered creature survives") {
                val game = board(1 to "Hill Giant").withCardInHand(1, "Day of Judgment")
                    .withLandsOnBattlefield(1, "Plains", 4).build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.givePlusOneCounter(bears)

                game.castSpell(1, "Day of Judgment").error shouldBe null
                game.resolveStack()

                withClue("McCoy is destroyed alongside, yet still replaces the simultaneous destruction") {
                    game.isInGraveyard(1, "Dr. McCoy, Surly Surgeon") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.plusOneCounters(bears) shouldBe 0
            }

            test("McCoy doesn't protect itself") {
                val game = board().withCardInHand(1, "Doom Blade").withLandsOnBattlefield(1, "Swamp", 2).build()
                val mccoy = game.findPermanent("Dr. McCoy, Surly Surgeon")!!
                game.givePlusOneCounter(mccoy)

                game.castSpell(1, "Doom Blade", mccoy).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Dr. McCoy, Surly Surgeon") shouldBe true
            }

            test("an opponent's creature with a +1/+1 counter isn't protected") {
                val game = board(2 to "Hill Giant").withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2).build()
                val giant = game.findPermanent("Hill Giant")!!
                game.givePlusOneCounter(giant)

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
