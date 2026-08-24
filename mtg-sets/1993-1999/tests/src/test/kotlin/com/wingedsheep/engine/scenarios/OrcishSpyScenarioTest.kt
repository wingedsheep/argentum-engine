package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Orcish Spy (Fallen Empires).
 *
 * "{T}: Look at the top three cards of target player's library."
 *
 * A look must leave the library exactly as it found it. The card used to gather the top three and
 * then "put them back" on top — but the gather never removed them, so the put-back relocated each
 * card individually, and placing on top prepends: the top three came back reversed. Nothing failed,
 * because nothing about a wrong library order is checked anywhere else.
 */
class OrcishSpyScenarioTest : ScenarioTestBase() {

    init {
        context("Orcish Spy") {

            test("looking leaves the target's library order untouched") {
                val game = scenario()
                    .withPlayers("Spy", "Victim")
                    .withCardOnBattlefield(1, "Orcish Spy", summoningSickness = false)
                    // Library is built top-first, so these are the top three in order.
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Elvish Warrior")
                    .withCardInLibrary(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun libraryNames(): List<String> = game.state
                    .getZone(ZoneKey(game.player2Id, Zone.LIBRARY))
                    .map { game.state.getEntity(it)?.get<CardComponent>()?.name ?: "?" }

                val before = libraryNames()
                withClue("sanity: the fixture built the library we think it did") {
                    before.take(3) shouldBe listOf("Grizzly Bears", "Hill Giant", "Elvish Warrior")
                }

                val spy = game.findPermanent("Orcish Spy")!!
                val abilityId = cardRegistry.getCard("Orcish Spy")!!.script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spy,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("a look changes nothing — not the order, not the count") {
                    libraryNames() shouldBe before
                }
                withClue("and nothing left the library") {
                    game.graveyardSize(2) shouldBe 0
                }
            }
        }
    }
}
