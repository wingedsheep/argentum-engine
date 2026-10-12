package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Astor, Bearer of Blades — {2}{R}{W} 4/4 legendary.
 *
 * - ETB: look at the top seven, may reveal an Equipment or Vehicle card into hand, rest on the
 *   bottom in a random order.
 * - Equipment you control have equip {1} — a second equip ability beside the printed one.
 * - Vehicles you control have crew 1 — a second crew ability beside the printed one.
 */
class AstorBearerOfBladesScenarioTest : ScenarioTestBase() {

    init {
        test("ETB puts a revealed Equipment into hand and bottoms the rest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Astor, Bearer of Blades")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Vulshok Gauntlets")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gauntlets = game.findCardsInLibrary(1, "Vulshok Gauntlets").single()

            game.castSpell(1, "Astor, Bearer of Blades").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(gauntlets)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Vulshok Gauntlets") shouldBe true
            withClue("the other two looked-at cards stay in the library") {
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
            }
        }

        test("an Equipment with equip {3} can be equipped for {1}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Astor, Bearer of Blades")
                .withCardOnBattlefield(1, "Vulshok Gauntlets")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val astor = cardRegistry.getCard("Astor, Bearer of Blades")!!
            val grantedEquip = astor.script.staticAbilities
                .filterIsInstance<GrantActivatedAbility>().single().ability.id
            val gauntlets = game.findPermanent("Vulshok Gauntlets")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, gauntlets, grantedEquip, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(gauntlets)?.get<AttachedToComponent>()?.targetId shouldBe bears
        }

        test("a Vehicle with crew 3 can be crewed by a 1-power creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Astor, Bearer of Blades")
                .withCardOnBattlefield(1, "Raiders' Karve")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karve = game.findPermanent("Raiders' Karve")!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.execute(CrewVehicle(game.player1Id, karve, listOf(lions))).error shouldBe null
            game.resolveStack()

            game.state.projectedState.isCreature(karve) shouldBe true
        }

        test("without Astor the Vehicle still needs crew 3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Raiders' Karve")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karve = game.findPermanent("Raiders' Karve")!!
            val lions = game.findPermanent("Savannah Lions")!!

            (game.execute(CrewVehicle(game.player1Id, karve, listOf(lions))).error != null) shouldBe true
        }
    }
}
