package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Neheb, Dreadhorde Champion — on combat damage to a player, discard any number, draw that many and
 * add that much {R}; the mana is kept as steps and phases end, so it survives into the postcombat
 * main phase. Discarding zero draws and adds nothing.
 */
class NehebDreadhordeChampionScenarioTest : ScenarioTestBase() {

    init {
        fun build() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Neheb, Dreadhorde Champion")
            .withCardsInHand(1, "Grizzly Bears", 3)
            .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.keptRed() = state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana
            .count { it.color == Color.RED && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }

        test("discarding two draws two and adds {R}{R} that lasts into the postcombat main phase") {
            val game = build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Neheb, Dreadhorde Champion" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2)).error shouldBe null
            game.resolveStack()

            withClue("Neheb dealt 5, two cards discarded and two drawn") {
                game.getLifeTotal(2) shouldBe 15
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 2
                game.findCardsInHand(1, "Island").size shouldBe 2
            }
            game.keptRed() shouldBe 2

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("the {R}{R} survived the end of combat") { game.keptRed() shouldBe 2 }
        }

        test("discarding zero draws nothing and adds no mana") {
            val game = build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Neheb, Dreadhorde Champion" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 3
            game.findCardsInHand(1, "Island").size shouldBe 0
            game.keptRed() shouldBe 0
        }
    }
}
