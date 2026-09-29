package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Doomskar Warrior (MOM #185) — {2}{G}{G} 4/3. Backup 1, trample. Whenever this creature deals
 * combat damage to a player or battle, look at that many cards from the top of your library; you
 * may reveal a creature or land card among them and put it into your hand; the rest go on the
 * bottom in a random order.
 */
class DoomskarWarriorScenarioTest : ScenarioTestBase() {
    init {
        context("Doomskar Warrior") {

            fun libraryNames(game: TestGame, player: Int): List<String> {
                val playerId = if (player == 1) game.player1Id else game.player2Id
                return game.state.getZone(ZoneKey(playerId, Zone.LIBRARY)).mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                }
            }

            test("combat damage looks at that many cards; a creature or land may go to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomskar Warrior", summoningSickness = false)
                    // Top of library first: four looked at (4 damage), the fifth is untouched.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Giant Growth")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Doomskar Warrior" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
                val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                select.minSelections shouldBe 0
                select.maxSelections shouldBe 1
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").first()
                val forest = game.findCardsInLibrary(1, "Forest").first()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").first()
                val growth = game.findCardsInLibrary(1, "Giant Growth").first()
                select.options shouldContainExactlyInAnyOrder listOf(bears, forest)
                select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bolt, growth)

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears put into hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
                val library = libraryNames(game, 1)
                library.first() shouldBe "Hill Giant"
                library.takeLast(3) shouldContainExactlyInAnyOrder listOf("Lightning Bolt", "Forest", "Giant Growth")
            }

            test("declining the reveal puts every looked-at card on the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomskar Warrior", summoningSickness = false)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Doomskar Warrior" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe false
                val library = libraryNames(game, 1)
                library.first() shouldBe "Hill Giant"
                library.takeLast(4) shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Forest", "Forest", "Forest")
            }

            test("backup on another creature grants the counter, trample and the dig trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Doomskar Warrior")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Doomskar Warrior").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.state.getEntity(bears)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.hasKeyword(bears, com.wingedsheep.sdk.core.Keyword.TRAMPLE) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                // The 3/3 Bears dealt 3 → look at exactly the top three cards.
                val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val hillGiant = game.findCardsInLibrary(1, "Hill Giant").first()
                val forest = game.findCardsInLibrary(1, "Forest").first()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").first()
                (select.options + select.nonSelectableOptions) shouldContainExactlyInAnyOrder listOf(forest, bolt, hillGiant)
                game.selectCards(listOf(hillGiant)).error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Hill Giant") shouldBe true
                libraryNames(game, 1).first() shouldBe "Swamp"
            }
        }
    }
}
