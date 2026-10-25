package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jaheira, Friend of the Forest (CLB #237).
 *
 * "Tokens you control have '{T}: Add {G}.'" — every token, not just creature tokens (ruling). A
 * noncreature token can use it the turn it arrives; a creature token can't without haste; a nontoken
 * permanent never has it.
 */
class JaheiraFriendOfTheForestScenarioTest : ScenarioTestBase() {

    private val grantedId by lazy {
        cardRegistry.requireCard("Jaheira, Friend of the Forest").staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id
    }

    private fun TestGame.greenInPool(): Int =
        state.getEntity(player1Id)?.get<ManaPoolComponent>()?.getAmount(Color.GREEN) ?: 0

    init {
        test("a Treasure token that just arrived taps for {G} without being sacrificed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jaheira, Friend of the Forest")
                .withCardOnBattlefield(1, "Treasure", summoningSickness = true, isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val treasure = game.findPermanent("Treasure")!!

            game.execute(ActivateAbility(game.player1Id, treasure, grantedId)).error shouldBe null

            game.greenInPool() shouldBe 1
            withClue("tapping it is not the Treasure's own sacrifice ability") {
                game.findPermanent("Treasure") shouldBe treasure
            }
        }

        test("a creature token taps for {G} once it has been under your control since your turn began") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jaheira, Friend of the Forest")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Grizzly Bears")!!, grantedId))
                .error shouldBe null
            game.greenInPool() shouldBe 1
        }

        test("a summoning-sick creature token can't use it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jaheira, Friend of the Forest")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true, isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Grizzly Bears")!!, grantedId))
                .error shouldNotBe null
            game.greenInPool() shouldBe 0
        }

        test("a nontoken creature doesn't have the ability") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Jaheira, Friend of the Forest")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Grizzly Bears")!!, grantedId))
                .error shouldNotBe null
            game.greenInPool() shouldBe 0
        }
    }
}
