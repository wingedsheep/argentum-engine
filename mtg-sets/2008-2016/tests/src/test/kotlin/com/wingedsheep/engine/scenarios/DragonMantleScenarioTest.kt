package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.DragonMantle
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dragon Mantle (THS #119) — {R} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, draw a card.
 *   Enchanted creature has "{R}: This creature gets +1/+0 until end of turn."
 */
class DragonMantleScenarioTest : ScenarioTestBase() {
    private val grantedId = DragonMantle.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().single().ability.id

    init {
        test("draws a card on entering, and the enchanted creature firebreathes") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Dragon Mantle")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Dragon Mantle", bears).error shouldBe null
            game.resolveStack()

            withClue("the enters trigger drew the Island") { game.isInHand(1, "Island") shouldBe true }

            repeat(2) {
                game.execute(ActivateAbility(game.player1Id, bears, grantedId)).error shouldBe null
                game.resolveStack()
            }

            withClue("two activations make the Bears a 4/2") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }
    }
}
