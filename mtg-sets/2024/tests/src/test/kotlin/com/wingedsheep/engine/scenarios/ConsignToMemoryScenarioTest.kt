package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Consign to Memory (MH3) — "Replicate {1}. Counter target triggered ability or colorless spell."
 */
class ConsignToMemoryScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCard(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.castConsign(target: EntityId, times: Int? = null) = execute(
        CastSpell(
            player1Id, handCard("Consign to Memory"),
            targets = listOf(ChosenTarget.Spell(target)),
            declaredCostSlot = times?.let { ChoiceSlot.REPLICATED },
            declaredCostTimes = times ?: 1,
        )
    )

    init {
        context("Consign to Memory") {
            test("counters a colorless spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ornithopter")
                    .withCardInHand(1, "Consign to Memory")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ornithopter").error shouldBe null
                val ornithopter = game.state.stack.single()
                game.castConsign(ornithopter).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe false
                game.isInGraveyard(1, "Ornithopter") shouldBe true
            }

            test("can't target a colored spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Consign to Memory")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.castConsign(game.state.stack.single()).error shouldNotBe null
            }

            test("replicated once, the copy counters a second triggered ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soul Warden")
                    .withCardOnBattlefield(1, "Soul Warden")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Consign to Memory")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.passPriority()
                val triggers = game.state.stack.filter {
                    game.state.getEntity(it)?.has<TriggeredAbilityOnStackComponent>() == true
                }
                triggers.size shouldBe 2

                game.castConsign(triggers[0], times = 1).error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision()
                (decision is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(triggers[1])).error shouldBe null
                game.resolveStack()

                game.state.stack.isEmpty() shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
