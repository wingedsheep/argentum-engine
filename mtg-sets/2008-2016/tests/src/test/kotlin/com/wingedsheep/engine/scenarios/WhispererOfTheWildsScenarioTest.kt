package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.frf.cards.WhispererOfTheWilds
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Whisperer of the Wilds (FRF #144) — {1}{G} Creature — Human Shaman 0/2.
 *
 *   {T}: Add {G}.
 *   Ferocious — {T}: Add {G}{G}. Activate only if you control a creature with power 4 or greater.
 */
class WhispererOfTheWildsScenarioTest : ScenarioTestBase() {

    private val name = "Whisperer of the Wilds"
    private val plainManaId = WhispererOfTheWilds.activatedAbilities[0].id
    private val ferociousId = WhispererOfTheWilds.activatedAbilities[1].id

    private fun green(game: TestGame) =
        game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Whisperer of the Wilds") {

            test("taps for {G}") {
                val game = board().withCardOnBattlefield(1, name).build()
                val whisperer = game.findPermanent(name)!!

                game.execute(ActivateAbility(game.player1Id, whisperer, plainManaId)).error shouldBe null
                green(game) shouldBe 1
            }

            test("ferocious taps for {G}{G} when you control a creature with power 4 or greater") {
                val game = board()
                    .withCardOnBattlefield(1, name)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .build()
                val whisperer = game.findPermanent(name)!!

                game.execute(ActivateAbility(game.player1Id, whisperer, ferociousId)).error shouldBe null
                green(game) shouldBe 2
            }

            test("ferocious can't be activated without a creature with power 4 or greater") {
                val game = board()
                    .withCardOnBattlefield(1, name)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
                val whisperer = game.findPermanent(name)!!

                withClue("a 3/3 of yours and an opponent's 6/4 don't satisfy 'you control … power 4 or greater'") {
                    game.execute(ActivateAbility(game.player1Id, whisperer, ferociousId)).error shouldNotBe null
                    green(game) shouldBe 0
                }
            }
        }
    }
}
