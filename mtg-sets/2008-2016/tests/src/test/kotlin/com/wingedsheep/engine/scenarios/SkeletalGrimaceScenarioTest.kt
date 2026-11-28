package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skeletal Grimace (ISD #116) — {1}{B} Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +1/+1 and has "{B}: Regenerate this creature."
 */
class SkeletalGrimaceScenarioTest : ScenarioTestBase() {

    init {
        val regenId = cardRegistry.getCard("Skeletal Grimace")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("enchanted creature gets +1/+1 and its granted {B} regenerate survives a destroy effect") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Skeletal Grimace", "Grizzly Bears")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3

            val regen = game.execute(ActivateAbility(game.player1Id, bears, regenId))
            withClue("granted regenerate should activate: ${regen.error}") { regen.error shouldBe null }
            game.resolveStack()

            val blade = game.castSpell(1, "Doom Blade", bears)
            withClue("Doom Blade should cast: ${blade.error}") { blade.error shouldBe null }
            game.resolveStack()

            withClue("the regeneration shield replaces the destruction") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
        }
    }
}
