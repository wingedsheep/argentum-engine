package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.VulshokSplitter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vulshok Splitter (ONE #156) — For Mirrodin! makes a 2/2 red Rebel wearing the Splitter (+2/+0);
 * Equip {2}{R} then moves it onto another creature, leaving the Rebel a plain 2/2.
 */
class VulshokSplitterScenarioTest : ScenarioTestBase() {

    init {
        context("Vulshok Splitter") {
            test("Rebel enters as a 4/2 and Equip {2}{R} moves the Splitter off it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Vulshok Splitter")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Vulshok Splitter")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val splitter = game.findPermanent("Vulshok Splitter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Equipped Rebel is 4/2") {
                    game.state.getEntity(splitter)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                    game.state.projectedState.getPower(rebel) shouldBe 4
                    game.state.projectedState.getToughness(rebel) shouldBe 2
                }

                val equipId = VulshokSplitter.activatedAbilities.first().id
                val equip = game.execute(
                    ActivateAbility(game.player1Id, splitter, equipId, targets = listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Splitter now equips Grizzly Bears (4/2); the Rebel is back to 2/2") {
                    game.state.getEntity(splitter)?.get<AttachedToComponent>()?.targetId shouldBe bears
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getPower(rebel) shouldBe 2
                    game.state.projectedState.getToughness(rebel) shouldBe 2
                }
            }
        }
    }
}
