package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.SoulOfInnistrad
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Soul of Innistrad: both abilities return up to three creature cards from your graveyard. The
 * graveyard ability's targets are chosen before its cost exiles the Soul, so the Soul itself may be
 * chosen — but it is in exile on resolution, an illegal target, and only the other cards return.
 */
class SoulOfInnistradScenarioTest : ScenarioTestBase() {
    init {
        test("on the battlefield: returns up to three target creature cards from your graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Soul of Innistrad")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .build()
            val soul = game.findPermanent("Soul of Innistrad")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.execute(
                ActivateAbility(
                    game.player1Id, soul, SoulOfInnistrad.activatedAbilities[0].id,
                    targets = listOf(
                        ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                        ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD),
                    ),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
        }

        test("from the graveyard: the Soul can be chosen, but it is exiled by the cost and stays exiled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, "Soul of Innistrad")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .build()
            val soul = game.findCardsInGraveyard(1, "Soul of Innistrad").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.execute(
                ActivateAbility(
                    game.player1Id, soul, SoulOfInnistrad.activatedAbilities[1].id,
                    targets = listOf(
                        ChosenTarget.Card(soul, game.player1Id, Zone.GRAVEYARD),
                        ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                        ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD),
                    ),
                )
            ).error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Soul of Innistrad") shouldBe true
            }
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
            withClue("the Soul was no longer in the graveyard on resolution, so it is not returned") {
                game.isInExile(1, "Soul of Innistrad") shouldBe true
                game.isInHand(1, "Soul of Innistrad") shouldBe false
            }
        }
    }
}
