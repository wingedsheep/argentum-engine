package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Mend the Wilds. */
class MendTheWildsScenarioTest : ScenarioTestBase() {

    private val lecternAbilityId by lazy {
        cardRegistry.requireCard("Living Lectern").activatedAbilities[0].id
    }

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun rolesAttachedTo(game: TestGame, roleName: String, host: EntityId): Int =
        game.findAllPermanents(roleName).count { role ->
            game.state.getEntity(role)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Mend the Wilds — returning a permanent card from your graveyard") {
            test("the targeted permanent card goes on top of your library and is drawn next") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Woodland Acolyte")
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val acolyte = game.findCardsInHand(1, "Woodland Acolyte").single()
                val wurm = game.findCardsInGraveyard(1, "Craw Wurm").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = acolyte,
                        targets = listOf(ChosenTarget.Card(wurm, game.player1Id, Zone.GRAVEYARD)),
                        faceIndex = 0,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Wurm left the graveyard for the top of the library") {
                    game.isInGraveyard(1, "Craw Wurm") shouldBe false
                    game.findCardsInLibrary(1, "Craw Wurm").size shouldBe 1
                    game.state.getLibrary(game.player1Id).first() shouldBe wurm
                }
                withClue("the Adventure exiled itself, so the creature is castable later") {
                    game.isInExile(1, "Woodland Acolyte") shouldBe true
                }
            }
        }
    }
}
