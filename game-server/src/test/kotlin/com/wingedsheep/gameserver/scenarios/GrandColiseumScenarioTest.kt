package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class GrandColiseumScenarioTest : ScenarioTestBase() {

    init {
        context("Grand Coliseum") {
            test("enters the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Grand Coliseum")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grand Coliseum"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Grand Coliseum should succeed") {
                    result.error shouldBe null
                }

                withClue("Grand Coliseum should be on battlefield") {
                    game.isOnBattlefield("Grand Coliseum") shouldBe true
                }

                val permanentId = game.findPermanent("Grand Coliseum")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Grand Coliseum should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can tap for colorless mana without dealing damage") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withLandsOnBattlefield(1, "Grand Coliseum", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)
                val coliseumId = game.findPermanent("Grand Coliseum")!!
                val coliseumDef = cardRegistry.getCard("Grand Coliseum")!!
                val colorlessManaAbility = coliseumDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = coliseumId,
                        abilityId = colorlessManaAbility.id
                    )
                )

                withClue("Activating colorless mana ability should succeed") {
                    result.error shouldBe null
                }

                withClue("Life total should not change when tapping for colorless") {
                    game.getLifeTotal(1) shouldBe initialLife
                }
            }

            test("tapping for any color deals 1 damage to controller") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withLandsOnBattlefield(1, "Grand Coliseum", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)
                val coliseumId = game.findPermanent("Grand Coliseum")!!
                val coliseumDef = cardRegistry.getCard("Grand Coliseum")!!
                val anyColorManaAbility = coliseumDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = coliseumId,
                        abilityId = anyColorManaAbility.id
                    )
                )

                withClue("Activating any-color mana ability should succeed") {
                    result.error shouldBe null
                }

                withClue("Should lose 1 life from tapping for any color") {
                    game.getLifeTotal(1) shouldBe initialLife - 1
                }
            }
        }
    }
}
