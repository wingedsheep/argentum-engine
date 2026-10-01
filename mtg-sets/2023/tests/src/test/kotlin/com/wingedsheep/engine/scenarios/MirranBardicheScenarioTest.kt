package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.MirranBardiche
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirran Bardiche (ONE #22) — For Mirrodin! makes a 2/2 red Rebel wearing the Bardiche
 * (+2/+1 and vigilance); Equip {3}{W} moves it, and the Rebel loses both the bonus and vigilance.
 */
class MirranBardicheScenarioTest : ScenarioTestBase() {

    init {
        context("Mirran Bardiche") {
            test("Rebel enters as a 4/3 with vigilance, and Equip {3}{W} moves both onto another creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Mirran Bardiche")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Mirran Bardiche")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val bardiche = game.findPermanent("Mirran Bardiche")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Equipped Rebel is a 4/3 with vigilance; Bears unaffected") {
                    game.state.getEntity(bardiche)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                    game.state.projectedState.getPower(rebel) shouldBe 4
                    game.state.projectedState.getToughness(rebel) shouldBe 3
                    game.state.projectedState.hasKeyword(rebel, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }

                val equipId = MirranBardiche.activatedAbilities.first().id
                val equip = game.execute(
                    ActivateAbility(game.player1Id, bardiche, equipId, targets = listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Bardiche now equips Grizzly Bears (4/3, vigilance); the Rebel is a plain 2/2") {
                    game.state.getEntity(bardiche)?.get<AttachedToComponent>()?.targetId shouldBe bears
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 3
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.getPower(rebel) shouldBe 2
                    game.state.projectedState.getToughness(rebel) shouldBe 2
                    game.state.projectedState.hasKeyword(rebel, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
