package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Swift Reconfiguration — {W} Aura with flash.
 *
 * "Enchant creature or Vehicle. Enchanted permanent is a Vehicle artifact with crew 5 and it loses
 * all other card types."
 *
 * Proves the layer-4 transform turns a creature into a noncreature Vehicle artifact, that the
 * granted crew 5 is a working crew ability on a permanent with no printed crew (crewing is a newer
 * type change, so it becomes a creature again with its own power and toughness), and that a
 * Vehicle keeps its printed crew beside the granted one.
 */
class SwiftReconfigurationScenarioTest : ScenarioTestBase() {

    init {
        test("enchanted creature becomes a noncreature Vehicle artifact with crew 5") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swift Reconfiguration")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Swift Reconfiguration", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("it loses the creature type and is only an artifact") {
                projected.isCreature(bears) shouldBe false
                projected.hasType(bears, "ARTIFACT") shouldBe true
            }
            withClue("its only subtype is Vehicle — the Bear creature type goes with the creature type") {
                projected.getSubtypes(bears) shouldBe setOf("Vehicle")
            }
            projected.getKeywords(bears).contains(KeywordAbility.grantedCrew(5)) shouldBe true
        }

        test("crewing the reconfigured creature makes it an artifact creature with its own power and toughness") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swift Reconfiguration")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanents("Savannah Lions")

            game.castSpell(1, "Swift Reconfiguration", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.isCreature(bears) shouldBe false

            withClue("total power 4 doesn't pay crew 5") {
                (game.execute(CrewVehicle(game.player1Id, bears, listOf(giant, lions.first()))).error != null) shouldBe true
            }

            game.execute(CrewVehicle(game.player1Id, bears, listOf(giant) + lions)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(bears) shouldBe true
            projected.hasType(bears, "ARTIFACT") shouldBe true
            projected.getPower(bears) shouldBe 2
            projected.getToughness(bears) shouldBe 2
        }

        test("an enchanted Vehicle keeps its printed crew beside the granted crew 5") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Swift Reconfiguration")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Raiders' Karve")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karve = game.findPermanent("Raiders' Karve")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Swift Reconfiguration", karve).error shouldBe null
            game.resolveStack()

            val keywords = game.state.projectedState.getKeywords(karve)
            keywords.contains(Keyword.CREW.name) shouldBe true
            keywords.contains(KeywordAbility.grantedCrew(5)) shouldBe true

            withClue("its printed crew 3 is still payable by a 3-power creature") {
                game.execute(CrewVehicle(game.player1Id, karve, listOf(giant))).error shouldBe null
                game.resolveStack()
                game.state.projectedState.isCreature(karve) shouldBe true
            }
        }
    }
}
