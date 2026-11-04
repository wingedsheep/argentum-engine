package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.AvidReclaimer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Avid Reclaimer (HOU #201) — {T}: Add {G} or {U}. If you control a Nissa planeswalker, you gain 2 life.
 *
 * The life gain rides on the mana ability itself, so it lands the moment the mana does, and only
 * while a Nissa planeswalker is on your side.
 */
class AvidReclaimerScenarioTest : ScenarioTestBase() {

    init {
        context("Avid Reclaimer") {

            test("adds {U} and gains 2 life while you control a Nissa planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Avid Reclaimer")
                    .withCardOnBattlefield(1, "Nissa, Ascended Animist")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val reclaimer = game.findPermanent("Avid Reclaimer")!!
                game.execute(
                    ActivateAbility(game.player1Id, reclaimer, AvidReclaimer.activatedAbilities[1].id)
                ).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("the blue ability adds {U}") { pool.blue shouldBe 1 }
                withClue("Nissa is on the battlefield, so 2 life is gained at once") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("adds {G} without gaining life when you control no Nissa planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Avid Reclaimer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val reclaimer = game.findPermanent("Avid Reclaimer")!!
                game.execute(
                    ActivateAbility(game.player1Id, reclaimer, AvidReclaimer.activatedAbilities[0].id)
                ).error shouldBe null

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                pool.green shouldBe 1
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
