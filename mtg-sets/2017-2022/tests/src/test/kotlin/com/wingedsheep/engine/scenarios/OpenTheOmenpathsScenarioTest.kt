package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Open the Omenpaths (KHM #143) — {2}{R} Instant.
 *
 *   Choose one —
 *   • Add two mana of any one color and two mana of any other color. Spend this mana only to cast
 *     creature or enchantment spells.
 *   • Creatures you control get +1/+0 until end of turn.
 *
 * The mana mode asks for two colors in turn; the second prompt doesn't offer the first color, so
 * the four mana are always two distinct colors. The mana then pays a creature spell but not an
 * instant.
 */
class OpenTheOmenpathsScenarioTest : ScenarioTestBase() {

    init {
        context("Open the Omenpaths — two mana of one color and two of another") {

            test("adds 2+2 mana of two distinct colors, spendable only on creature or enchantment spells") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Open the Omenpaths")
                    .withCardInHand(1, "Giant Growth")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Open the Omenpaths", modeIndex = 0).error shouldBe null
                game.resolveStack()

                val first = game.getPendingDecision() as? ChooseColorDecision
                first.shouldNotBeNull()
                first.availableColors shouldBe Color.entries.toSet()
                game.submitDecision(ColorChosenResponse(first.id, Color.GREEN)).error shouldBe null

                val second = game.getPendingDecision() as? ChooseColorDecision
                second.shouldNotBeNull()
                withClue("the other color can't be the first one") {
                    second.availableColors shouldBe setOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED)
                }
                game.submitDecision(ColorChosenResponse(second.id, Color.WHITE)).error shouldBe null
                game.getPendingDecision() shouldBe null

                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                pool.restrictedMana.count { it.color == Color.GREEN } shouldBe 2
                pool.restrictedMana.count { it.color == Color.WHITE } shouldBe 2

                withClue("the mana can't pay for an instant") {
                    game.castSpell(1, "Giant Growth", game.findPermanent("Savannah Lions")).error shouldNotBe null
                }
                game.castSpell(1, "Grizzly Bears").error shouldBe null
            }
        }
    }
}
