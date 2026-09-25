package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.SerpentSkin
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Serpent Skin (CHK #240) — "Flash / Enchant creature / Enchanted creature gets +1/+1. /
 * {G}: Regenerate enchanted creature."
 *
 * The regeneration ability lives on the Aura, not the creature, so the shield it makes has to land
 * on the host.
 */
class SerpentSkinScenarioTest : ScenarioTestBase() {

    private val abilityId = SerpentSkin.activatedAbilities.single().id

    init {
        context("Serpent Skin") {

            test("the enchanted creature gets +1/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Serpent Skin", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }

            test("{G} regenerates the enchanted creature out of a destroy effect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Serpent Skin", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Rend Flesh") // sorcery: destroy target non-Spirit creature
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skin = game.findPermanent("Serpent Skin")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = skin, abilityId = abilityId)
                ).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Rend Flesh", bears).error shouldBe null
                game.resolveStack()

                withClue("the shield on the host replaces the destruction — it survives, tapped") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
