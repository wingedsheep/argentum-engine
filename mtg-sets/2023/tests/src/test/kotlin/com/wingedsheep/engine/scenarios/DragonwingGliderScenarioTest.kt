package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dragonwing Glider (ONE #128) — For Mirrodin! makes a 2/2 red Rebel and attaches to it;
 * the equipped creature gets +2/+2 and has flying and haste, so the fresh Rebel is a 4/4
 * flier that can attack the turn it's made. Other creatures get nothing.
 */
class DragonwingGliderScenarioTest : ScenarioTestBase() {

    init {
        context("Dragonwing Glider") {
            test("the Rebel it makes is a 4/4 flying haste attacker; an unequipped creature is unaffected") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Dragonwing Glider")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Dragonwing Glider")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val glider = game.findPermanent("Dragonwing Glider")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Glider is attached to the Rebel") {
                    game.state.getEntity(glider)?.get<AttachedToComponent>()?.targetId shouldBe rebel
                }

                val projected = game.state.projectedState
                withClue("Equipped Rebel is a 4/4 with flying and haste") {
                    projected.getPower(rebel) shouldBe 4
                    projected.getToughness(rebel) shouldBe 4
                    projected.hasKeyword(rebel, Keyword.FLYING) shouldBe true
                    projected.hasKeyword(rebel, Keyword.HASTE) shouldBe true
                }
                withClue("Unequipped Grizzly Bears stays a vanilla 2/2") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.FLYING) shouldBe false
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Rebel Token" to 2))
                withClue("Haste lets the brand-new Rebel attack: ${attack.error}") { attack.error shouldBe null }
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("Bob took 4 from the unblocked flier") { game.getLifeTotal(2) shouldBe 16 }
            }
        }
    }
}
