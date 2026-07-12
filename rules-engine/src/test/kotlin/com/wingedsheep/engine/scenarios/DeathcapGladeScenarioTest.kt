package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Deathcap Glade (VOW #261) — Land.
 *
 *   This land enters tapped unless you control two or more other lands.
 *   {T}: Add {B} or {G}.
 *
 * One of the "slow lands": enters untapped only with two or more other lands already in play
 * (i.e. three or more lands total, counting itself). Exercises both the tapped-entry replacement
 * and the two-mode {T} mana ability.
 */
class DeathcapGladeScenarioTest : ScenarioTestBase() {

    init {
        context("Deathcap Glade — enters tapped unless you control two or more other lands") {

            test("enters tapped as the very first land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deathcap Glade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glade = game.findCardsInHand(1, "Deathcap Glade").first()
                game.execute(PlayLand(game.player1Id, glade)).error shouldBe null

                withClue("With no other lands, Deathcap Glade enters tapped") {
                    game.state.getEntity(glade)?.has<TappedComponent>() shouldBe true
                }
            }

            test("enters untapped with two or more other lands already controlled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deathcap Glade")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glade = game.findCardsInHand(1, "Deathcap Glade").first()
                game.execute(PlayLand(game.player1Id, glade)).error shouldBe null

                withClue("With two other lands, Deathcap Glade enters untapped") {
                    game.state.getEntity(glade)?.has<TappedComponent>() shouldBe false
                }
            }

            test("{T}: Add {B} or {G}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deathcap Glade", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glade = game.findPermanent("Deathcap Glade")!!
                val blackAbility = cardRegistry.getCard("Deathcap Glade")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = glade, abilityId = blackAbility)
                ).error shouldBe null

                withClue("taps for black") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 1
                }
            }

            test("{T}: Add {G} on a second copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deathcap Glade", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glade = game.findPermanent("Deathcap Glade")!!
                val greenAbility = cardRegistry.getCard("Deathcap Glade")!!.activatedAbilities[1].id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = glade, abilityId = greenAbility)
                ).error shouldBe null

                withClue("taps for green") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.green shouldBe 1
                }
            }
        }
    }
}
