package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class RileScenarioTest : ScenarioTestBase() {
    init {
        test("damages your creature, draws, and grants trample only until cleanup") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rile")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Rile", bears).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            game.isInHand(1, "Forest") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }

        test("lethal damage does not stop the draw and the creature dies after the draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rile")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val elves = game.findPermanent("Llanowar Elves")!!
            game.castSpell(1, "Rile", elves).error shouldBe null
            val events = game.resolveStack().flatMap { it.events }
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            val drawIndex = events.indexOfFirst { it is CardsDrawnEvent }
            val deathIndex = events.indexOfFirst {
                it is ZoneChangeEvent && it.entityId == elves && it.toZone == Zone.GRAVEYARD
            }
            (drawIndex >= 0 && deathIndex > drawIndex) shouldBe true
        }

        test("an illegal target on resolution prevents drawing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rile")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Rile", bears).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Unsummon", bears).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Forest") shouldBe false
            game.isInGraveyard(1, "Rile") shouldBe true
        }

        test("cannot target an opponent's creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rile")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            (game.castSpell(1, "Rile", bears).error != null) shouldBe true
        }
    }
}
