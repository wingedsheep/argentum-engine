package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bonded Herdbeast // Plated Kilnbeast (MOM #178).
 *
 *   Front (4/5) — "{4}{R/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (7/5) — Menace.
 *
 * Covers the {R/P} pip paid with 2 life when no red source is available, paid with {R} when one is,
 * and the sorcery-speed restriction.
 */
class BondedHerdbeastScenarioTest : ScenarioTestBase() {

    init {
        context("Bonded Herdbeast") {

            test("a mono-green board flips it by paying 2 life for {R/P}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bonded Herdbeast")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Bonded Herdbeast")!!
                val abilityId = cardRegistry.getCard("Bonded Herdbeast")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = beast, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("{R/P} was paid with 2 life") { game.getLifeTotal(1) shouldBe 18 }
                withClue("transformed into a 7/5 menace Plated Kilnbeast") {
                    game.state.getEntity(beast)!!.get<CardComponent>()!!.name shouldBe "Plated Kilnbeast"
                    game.state.projectedState.getPower(beast) shouldBe 7
                    game.state.projectedState.getToughness(beast) shouldBe 5
                    game.state.projectedState.hasKeyword(beast, Keyword.MENACE) shouldBe true
                }
            }

            test("with a Mountain the pip is paid with red mana, not life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bonded Herdbeast")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Bonded Herdbeast")!!
                val abilityId = cardRegistry.getCard("Bonded Herdbeast")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = beast, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(beast)!!.get<CardComponent>()!!.name shouldBe "Plated Kilnbeast"
            }

            test("can't be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bonded Herdbeast")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Bonded Herdbeast")!!
                val abilityId = cardRegistry.getCard("Bonded Herdbeast")!!.activatedAbilities.first().id

                withClue("on the opponent's turn the sorcery-speed ability is rejected") {
                    game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = beast, abilityId = abilityId)
                    ).error shouldNotBe null
                }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
