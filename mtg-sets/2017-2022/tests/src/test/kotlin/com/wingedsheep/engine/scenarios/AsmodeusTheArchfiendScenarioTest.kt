package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Asmodeus the Archfiend (AFR #88).
 *
 *   Binding Contract — If you would draw a card, exile the top card of your library face down instead.
 *   {B}{B}{B}: Draw seven cards.
 *   {B}: Return all cards exiled with Asmodeus to their owner's hand and you lose that much life.
 */
class AsmodeusTheArchfiendScenarioTest : ScenarioTestBase() {

    init {
        fun activate(game: TestGame, index: Int) {
            val asmodeus = game.findPermanent("Asmodeus the Archfiend")!!
            val abilityId = cardRegistry.getCard("Asmodeus the Archfiend")!!.activatedAbilities[index].id
            val result = game.execute(ActivateAbility(game.player1Id, asmodeus, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
        }

        context("Asmodeus the Archfiend") {

            test("draw seven exiles seven face down; {B} returns them and costs that much life") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Asmodeus the Archfiend")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) { builder.withCardInLibrary(1, "Forest") }
                val game = builder.build()

                val libraryBefore = game.librarySize(1)
                val handBefore = game.handSize(1)

                activate(game, 0)

                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE)).toList()
                withClue("each of the seven draws is replaced by a face-down exile") {
                    exile.size shouldBe 7
                    exile.all { game.state.getEntity(it)!!.has<FaceDownComponent>() } shouldBe true
                    game.librarySize(1) shouldBe libraryBefore - 7
                    game.handSize(1) shouldBe handBefore
                }
                val asmodeus = game.findPermanent("Asmodeus the Archfiend")!!
                withClue("the exiled cards are linked to Asmodeus") {
                    game.state.getEntity(asmodeus)!!.get<LinkedExileComponent>()!!.exiledIds.toSet() shouldBe exile.toSet()
                }

                activate(game, 1)

                withClue("all seven come back to hand and you lose 7 life") {
                    game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE)).size shouldBe 0
                    game.handSize(1) shouldBe handBefore + 7
                    game.getLifeTotal(1) shouldBe 13
                }
            }

            test("with an empty library the replaced draw exiles nothing and you don't lose") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Asmodeus the Archfiend")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activate(game, 0)

                withClue("no card was drawn from the empty library, so no state-based loss") {
                    game.state.gameOver shouldBe false
                }

                activate(game, 1)
                withClue("nothing was exiled with Asmodeus, so no life is lost") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
