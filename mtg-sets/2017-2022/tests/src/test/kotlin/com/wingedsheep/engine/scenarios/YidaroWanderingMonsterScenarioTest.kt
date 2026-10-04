package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Yidaro, Wandering Monster {5}{R}{R} — Cycling {1}{R}. "When you cycle this card, shuffle it into
 * your library from your graveyard. If you've cycled a card named Yidaro, Wandering Monster four or
 * more times this game, put it onto the battlefield from your graveyard instead."
 *
 * Ruling: any card named Yidaro counts, not just the same physical card — and only cards with that
 * name count.
 */
class YidaroWanderingMonsterScenarioTest : ScenarioTestBase() {

    private val yidaro = "Yidaro, Wandering Monster"

    private fun TestGame.cycle(name: String) {
        withClue("cycling $name") { cycleCard(1, name).error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun base(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("the first cycle shuffles it from the graveyard into the library and draws a card") {
            val game = base(2).withCardInHand(1, yidaro).build()

            game.cycle(yidaro)

            game.isInGraveyard(1, yidaro) shouldBe false
            game.findCardsInLibrary(1, yidaro) shouldHaveSize 1
            game.isOnBattlefield(yidaro) shouldBe false
            game.isInHand(1, "Forest") shouldBe true
        }

        test("the fourth cycle of any card named Yidaro puts it onto the battlefield instead") {
            val game = base(8)
                .withCardInHand(1, yidaro)
                .withCardInHand(1, yidaro)
                .withCardInHand(1, yidaro)
                .withCardInHand(1, yidaro)
                .build()

            repeat(3) { n ->
                game.cycle(yidaro)
                withClue("cycle ${n + 1} shuffles it away") { game.isOnBattlefield(yidaro) shouldBe false }
            }
            withClue("three copies shuffled back into the library") {
                (game.findCardsInLibrary(1, yidaro).size + game.findCardsInHand(1, yidaro).size) shouldBe 4
            }

            game.cycle(yidaro)

            withClue("the fourth cycle puts it onto the battlefield") {
                game.isOnBattlefield(yidaro) shouldBe true
            }
            game.isInGraveyard(1, yidaro) shouldBe false
        }

        test("cycling other cards doesn't count toward Yidaro") {
            val game = base(5)
                .withCardInHand(1, "Startling Development")
                .withCardInHand(1, "Startling Development")
                .withCardInHand(1, "Startling Development")
                .withCardInHand(1, yidaro)
                .build()

            repeat(3) { game.cycle("Startling Development") }
            game.cycle(yidaro)

            game.isOnBattlefield(yidaro) shouldBe false
            game.findCardsInLibrary(1, yidaro) shouldHaveSize 1
        }

        test("if it left the graveyard before the trigger resolves, it stays where it is") {
            val game = base(2).withCardInHand(1, yidaro).build()

            withClue("cycling") { game.cycleCard(1, yidaro).error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            val card = game.findCardsInGraveyard(1, yidaro).single()
            withClue("the cycle trigger is waiting on the stack") { game.state.stack.isEmpty() shouldBe false }
            game.state = game.state.moveToZone(
                card,
                ZoneKey(game.player1Id, Zone.GRAVEYARD),
                ZoneKey(game.player1Id, Zone.EXILE)
            )
            game.resolveStack()

            game.isInExile(1, yidaro) shouldBe true
            game.findCardsInLibrary(1, yidaro) shouldHaveSize 0
        }
    }
}
