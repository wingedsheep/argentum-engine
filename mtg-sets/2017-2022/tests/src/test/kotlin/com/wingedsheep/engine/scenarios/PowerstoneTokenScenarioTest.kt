package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Brothers' War Powerstone token (`PredefinedTokens.Powerstone`, `Effects.CreatePowerstone`):
 * "{T}: Add {C}. This mana can't be spent to cast a nonartifact spell."
 *
 * The restriction forbids exactly one spend — casting a nonartifact spell — so the mana pays for
 * an artifact spell and for an activated ability (here an equip cost), but never for a creature
 * spell that isn't an artifact.
 */
class PowerstoneTokenScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        test("Powerstone mana pays for an artifact spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Powerstone", isToken = true)
                .withCardInHand(1, "Bonesplitter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bonesplitter").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Bonesplitter") shouldBe true
            withClue("the Powerstone paid the {1}") { game.isTapped("Powerstone") shouldBe true }
        }

        test("Powerstone mana can't be spent to cast a nonartifact spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Powerstone", isToken = true)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Forest + Powerstone can't pay {1}{G} for a nonartifact creature") {
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isTapped("Powerstone") shouldBe false
        }

        test("Powerstone mana pays for an activated ability") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Powerstone", isToken = true)
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bonesplitter = game.findPermanent("Bonesplitter")!!
            val equip = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<ActivateAbility>()
                .firstOrNull { it.sourceId == bonesplitter }
            withClue("equip {1} is affordable with only a Powerstone") { equip shouldNotBe null }

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(equip!!.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.resolveStack()

            game.isTapped("Powerstone") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 4
        }
    }
}
