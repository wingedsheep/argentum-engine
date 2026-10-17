package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.VivienOnTheHunt
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Vivien on the Hunt (SNC #162). The +2's sacrifice is an optional resolution-time choice whose
 * last-known mana value sets an *exact* search target (MV + 1); declining does nothing. The +1
 * mills five and returns any number of the milled creature cards; the −1 makes a 4/4 Rhino Warrior.
 */
class VivienOnTheHuntScenarioTest : ScenarioTestBase() {

    private val plusTwo = VivienOnTheHunt.activatedAbilities[0].id
    private val plusOne = VivienOnTheHunt.activatedAbilities[1].id
    private val minusOne = VivienOnTheHunt.activatedAbilities[2].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        test("+2: sacrificing a 2-drop searches for a creature card with mana value exactly 3") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vivien on the Hunt")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Llanowar Elves")   // MV 1 — less, not equal
                .withCardInLibrary(1, "Centaur Courser")  // MV 3 — the only legal find
                .withCardInLibrary(1, "Hill Giant")       // MV 4 — more, not equal
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vivien = game.findPermanent("Vivien on the Hunt")!!
            seedLoyalty(game, vivien, 4)
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findCardsInLibrary(1, "Centaur Courser").single()

            game.execute(ActivateAbility(game.player1Id, vivien, plusTwo)).error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(bears)).error shouldBe null

            val search = game.getPendingDecision() as SelectCardsDecision
            withClue("only the mana value 3 creature card is findable") {
                search.options shouldContainExactlyInAnyOrder listOf(courser)
            }
            game.selectCards(listOf(courser)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.getZone(game.player1Id, Zone.BATTLEFIELD) shouldContain courser
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isOnBattlefield("Llanowar Elves") shouldBe false
            loyalty(game, vivien) shouldBe 6
        }

        test("+2: declining the sacrifice does nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vivien on the Hunt")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vivien = game.findPermanent("Vivien on the Hunt")!!
            seedLoyalty(game, vivien, 4)

            game.execute(ActivateAbility(game.player1Id, vivien, plusTwo)).error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            withClue("'If you do' never fired — no sacrifice, no search") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Centaur Courser") shouldBe false
                game.getPendingDecision() shouldBe null
            }
            loyalty(game, vivien) shouldBe 6
        }

        test("+1: mills five and returns only the chosen creature cards") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vivien on the Hunt")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Llanowar Elves")   // sixth card — not milled
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vivien = game.findPermanent("Vivien on the Hunt")!!
            seedLoyalty(game, vivien, 4)
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            val courser = game.findCardsInLibrary(1, "Centaur Courser").single()

            game.execute(ActivateAbility(game.player1Id, vivien, plusOne)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            withClue("only the milled creature cards are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(bears, giant, courser)
            }
            game.selectCards(listOf(bears, courser)).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.HAND).toSet() shouldBe setOf(bears, courser)
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.findCardsInLibrary(1, "Llanowar Elves").size shouldBe 1
            loyalty(game, vivien) shouldBe 5
        }

        test("−1 creates a 4/4 green Rhino Warrior token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vivien on the Hunt")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vivien = game.findPermanent("Vivien on the Hunt")!!
            seedLoyalty(game, vivien, 4)

            game.execute(ActivateAbility(game.player1Id, vivien, minusOne)).error shouldBe null
            game.resolveStack()

            val rhino = game.findPermanent("Rhino Warrior Token")!!
            game.state.projectedState.getPower(rhino) shouldBe 4
            game.state.projectedState.getToughness(rhino) shouldBe 4
            loyalty(game, vivien) shouldBe 3
        }
    }
}
