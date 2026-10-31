package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tale's End (M20) — "Counter target activated ability, triggered ability, or legendary spell."
 */
class TalesEndScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTalesEnd(target: EntityId) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Tale's End" },
            targets = listOf(ChosenTarget.Spell(target)),
        )
    )

    init {
        context("Tale's End") {
            test("counters a legendary spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Isamaru, Hound of Konda")
                    .withCardInHand(1, "Tale's End")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Isamaru, Hound of Konda").error shouldBe null
                game.castTalesEnd(game.state.stack.single()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Isamaru, Hound of Konda") shouldBe false
                game.isInGraveyard(1, "Isamaru, Hound of Konda") shouldBe true
            }

            test("can't target a nonlegendary spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Tale's End")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.castTalesEnd(game.state.stack.single()).error shouldNotBe null
            }

            test("counters a triggered ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soul Warden")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Tale's End")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.passPriority()
                val trigger = game.state.stack.single {
                    game.state.getEntity(it)?.has<TriggeredAbilityOnStackComponent>() == true
                }

                game.castTalesEnd(trigger).error shouldBe null
                game.resolveStack()

                game.state.stack.isEmpty() shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
