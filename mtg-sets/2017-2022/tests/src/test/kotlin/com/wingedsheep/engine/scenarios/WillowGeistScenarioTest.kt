package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Willow Geist (MID #207) — {G} Creature — Treefolk Spirit, 1/1, Trample.
 *
 *   Whenever one or more cards leave your graveyard, put a +1/+1 counter on this creature.
 *   When this creature dies, you gain life equal to its power.
 *
 * Tormod's Crypt exiles several cards from its controller's graveyard at once: the batching
 * trigger adds exactly one counter. Killing the grown Geist then gains life equal to its
 * last-known power (counters included).
 */
class WillowGeistScenarioTest : ScenarioTestBase() {

    init {
        context("Willow Geist") {

            test("several cards leaving at once add one counter; dying gains life equal to its last power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Willow Geist")
                    .withCardOnBattlefield(1, "Tormod's Crypt")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val geist = game.findPermanent("Willow Geist")!!
                val crypt = game.findPermanent("Tormod's Crypt")!!
                val cryptAbility = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crypt,
                        abilityId = cryptAbility,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("graveyard was exiled") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("one batch of cards leaving adds exactly one +1/+1 counter") {
                    game.state.getEntity(geist)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("Willow Geist is now 2/2") {
                    game.state.projectedState.getPower(geist) shouldBe 2
                }

                game.castSpell(1, "Doom Blade", geist).error shouldBe null
                game.resolveStack()

                withClue("Willow Geist died") {
                    game.isOnBattlefield("Willow Geist") shouldBe false
                }
                withClue("dies trigger gains life equal to its last-known power (2)") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
