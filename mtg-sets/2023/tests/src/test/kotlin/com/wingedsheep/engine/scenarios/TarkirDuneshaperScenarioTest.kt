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
 * Tarkir Duneshaper // Burnished Dunestomper (MOM #43).
 *
 *   Front (1/2) — "{4}{G/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (4/3) — Trample.
 *
 * Covers the {G/P} pip paid with 2 life when no green source is available, paid with {G} when one is,
 * and the sorcery-speed restriction.
 */
class TarkirDuneshaperScenarioTest : ScenarioTestBase() {

    init {
        context("Tarkir Duneshaper") {

            test("a board with no green source flips it by paying 2 life for {G/P}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tarkir Duneshaper")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Tarkir Duneshaper")!!
                val abilityId = cardRegistry.getCard("Tarkir Duneshaper")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = beast, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("{G/P} was paid with 2 life") { game.getLifeTotal(1) shouldBe 18 }
                withClue("transformed into a 4/3 trample Burnished Dunestomper") {
                    game.state.getEntity(beast)!!.get<CardComponent>()!!.name shouldBe "Burnished Dunestomper"
                    game.state.projectedState.getPower(beast) shouldBe 4
                    game.state.projectedState.getToughness(beast) shouldBe 3
                    game.state.projectedState.hasKeyword(beast, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("with a Forest the pip is paid with green mana, not life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tarkir Duneshaper")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Tarkir Duneshaper")!!
                val abilityId = cardRegistry.getCard("Tarkir Duneshaper")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = beast, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(beast)!!.get<CardComponent>()!!.name shouldBe "Burnished Dunestomper"
            }

            test("can't be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tarkir Duneshaper")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beast = game.findPermanent("Tarkir Duneshaper")!!
                val abilityId = cardRegistry.getCard("Tarkir Duneshaper")!!.activatedAbilities.first().id

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
