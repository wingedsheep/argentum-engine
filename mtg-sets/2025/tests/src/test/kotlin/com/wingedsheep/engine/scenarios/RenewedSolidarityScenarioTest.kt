package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Renewed Solidarity (DRC #7) — {2}{W} Enchantment.
 *
 * "As this enchantment enters, choose a creature type.
 *  Creatures you control of the chosen type get +1/+0.
 *  At the beginning of your end step, for each token you control of the chosen type that entered
 *  this turn, create a token that's a copy of it."
 *
 * Pins the end-step composition: the chosen type is read off the enchantment inside a
 * `ForEachInGroup`, only tokens of that type that entered this turn are copied, and the copies
 * made during the resolution are not themselves copied again.
 */
class RenewedSolidarityScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseType(type: String) {
        val solidarity = findPermanent("Renewed Solidarity")!!
        state = state.updateEntity(solidarity) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    private fun TestGame.tokensWithSubtype(subtype: String) =
        state.getBattlefield().filter {
            state.getEntity(it)?.has<TokenComponent>() == true &&
                state.projectedState.hasSubtype(it, subtype)
        }

    init {
        test("pumps chosen-type creatures and copies each chosen-type token that entered this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Renewed Solidarity")
                .withCardInHand(1, "Dragon Fodder")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.chooseType("Goblin")

            game.castSpell(1, "Dragon Fodder").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            val goblins = game.tokensWithSubtype("Goblin")
            withClue("two Goblin tokens, each a 2/1 from the +1/+0") {
                goblins.size shouldBe 2
                goblins.forEach {
                    game.state.projectedState.getPower(it) shouldBe 2
                    game.state.projectedState.getToughness(it) shouldBe 1
                }
            }
            withClue("Soldiers aren't the chosen type and get no bonus") {
                game.tokensWithSubtype("Soldier").forEach {
                    game.state.projectedState.getPower(it) shouldBe 1
                }
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("each Goblin token that entered this turn is copied once — the new copies aren't copied again") {
                game.tokensWithSubtype("Goblin").size shouldBe 4
            }
            withClue("tokens of another type are not copied") {
                game.tokensWithSubtype("Soldier").size shouldBe 2
            }
        }
    }
}
