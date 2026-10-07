package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UnnaturalAggression
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class UnnaturalAggressionScenarioTest : ScenarioTestBase() {
    init {
        fun board(yours: String = "Hill Giant", theirs: String = "Grizzly Bears"): TestGame = scenario()
            .withPlayers("P1", "P2")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInHand(1, "Unnatural Aggression")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInHand(1, "Unsummon")
            .withCardOnBattlefield(1, yours)
            .withCardOnBattlefield(2, theirs)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun cast(game: TestGame, yours: String = "Hill Giant", theirs: String = "Grizzly Bears") {
            game.execute(CastSpell(
                game.player1Id,
                game.findCardsInHand(1, "Unnatural Aggression").single(),
                listOf(
                    ChosenTarget.Permanent(game.findPermanent(yours)!!),
                    ChosenTarget.Permanent(game.findPermanent(theirs)!!),
                ),
            )).error shouldBe null
        }

        fun resolve(game: TestGame) {
            game.resolveStack().forEach { it.error shouldBe null }
            game.state.stack.isEmpty() shouldBe true
        }

        test("devoid makes the spell colorless despite its green mana cost") {
            UnnaturalAggression.colors.isEmpty() shouldBe true
        }

        test("lethal fight damage exiles the opponent's creature") {
            val game = board()
            cast(game)
            resolve(game)
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("a creature that survives the fight is exiled if it dies later that turn") {
            val game = board("Grizzly Bears", "Hill Giant")
            cast(game, "Grizzly Bears", "Hill Giant")
            resolve(game)
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
            resolve(game)
            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("the exile replacement expires at the end of the turn") {
            val game = board("Grizzly Bears", "Hill Giant")
            cast(game, "Grizzly Bears", "Hill Giant")
            resolve(game)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            if (game.state.priorityPlayerId != game.player1Id) {
                game.passPriority().error shouldBe null
            }
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
            resolve(game)
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe false
        }

        test("an illegal friendly target prevents fighting but leaves the opponent marked for exile") {
            val game = board()
            cast(game)
            game.castSpell(1, "Unsummon", game.findPermanent("Hill Giant")!!).error shouldBe null
            resolve(game)
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findCardsInHand(1, "Hill Giant").size shouldBe 1
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            resolve(game)
            game.isInExile(2, "Grizzly Bears") shouldBe true
        }

        test("an illegal opposing target prevents fighting") {
            val game = board("Grizzly Bears", "Hill Giant")
            cast(game, "Grizzly Bears", "Hill Giant")
            game.castSpell(1, "Unsummon", game.findPermanent("Hill Giant")!!).error shouldBe null
            resolve(game)
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findCardsInHand(2, "Hill Giant").size shouldBe 1
        }
    }
}
