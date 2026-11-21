package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FleshBloodScenarioTest : ScenarioTestBase() {
    init {
        test("fused, Flesh's counters land before Blood reads the creature's power") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Flesh // Blood")
                .withLandsOnBattlefield(1, "Swamp", 1).withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Flesh // Blood"
            }
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(
                        // Flesh: creature card in a graveyard, then the creature getting counters.
                        ChosenTarget.Card(giant, game.player2Id, Zone.GRAVEYARD),
                        ChosenTarget.Permanent(bears),
                        // Blood: creature you control, then any target.
                        ChosenTarget.Permanent(bears),
                        ChosenTarget.Player(game.player2Id)
                    ),
                    castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            // Flesh exiled the 3-power Hill Giant and put three +1/+1 counters on the Bears.
            game.isInExile(2, "Hill Giant") shouldBe true
            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            // Blood then read the Bears' power as 5, not 2.
            game.getLifeTotal(2) shouldBe 15
            game.isInGraveyard(1, "Flesh // Blood") shouldBe true
        }

        test("Blood alone has the creature deal damage equal to its power") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Flesh // Blood")
                .withLandsOnBattlefield(1, "Mountain", 1).withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Flesh // Blood"
            }

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(
                        ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!),
                        ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)
                    ),
                    faceIndex = 1
                )
            ).error.shouldBeNull()
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            (game.findPermanent("Hill Giant") != null) shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
