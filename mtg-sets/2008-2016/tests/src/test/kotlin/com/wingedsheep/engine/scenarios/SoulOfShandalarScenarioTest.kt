package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.SoulOfShandalar
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Soul of Shandalar: the battlefield ability hits a player and up to one of their creatures; the
 * graveyard ability exiles the card as its cost and the exiled card still deals the damage — here
 * to a planeswalker and a creature that planeswalker's controller controls.
 */
class SoulOfShandalarScenarioTest : ScenarioTestBase() {
    init {
        test("on the battlefield: 3 to the targeted player and 3 to a creature they control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Soul of Shandalar")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .build()
            val soul = game.findPermanent("Soul of Shandalar")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, soul, SoulOfShandalar.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.findCardsInGraveyard(2, "Grizzly Bears") shouldHaveSize 1
        }

        test("from the graveyard: exiled as the cost, it still damages a planeswalker and its controller's creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, "Soul of Shandalar")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Chandra, Pyromaster")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .build()
            val soul = game.findCardsInGraveyard(1, "Soul of Shandalar").single()
            val chandra = game.findPermanent("Chandra, Pyromaster")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val ability = SoulOfShandalar.activatedAbilities[1].id

            withClue("the creature must be controlled by the targeted planeswalker's controller") {
                game.execute(
                    ActivateAbility(
                        game.player1Id, soul, ability,
                        targets = listOf(ChosenTarget.Permanent(chandra), ChosenTarget.Permanent(giant)),
                    )
                ).error shouldNotBe null
            }

            game.execute(
                ActivateAbility(
                    game.player1Id, soul, ability,
                    targets = listOf(ChosenTarget.Permanent(chandra), ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Soul of Shandalar") shouldBe true
            }
            game.resolveStack()

            withClue("3 damage removed 3 of Chandra's 4 loyalty") {
                game.state.getEntity(chandra)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 1
            }
            game.findCardsInGraveyard(2, "Grizzly Bears") shouldHaveSize 1
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
