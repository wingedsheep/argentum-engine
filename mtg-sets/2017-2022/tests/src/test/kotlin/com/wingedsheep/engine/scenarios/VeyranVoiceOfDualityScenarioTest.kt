package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Veyran, Voice of Duality (C21 #3) — magecraft +1/+1, and "If you casting or copying an instant or
 * sorcery spell causes a triggered ability of a permanent you control to trigger, that ability triggers
 * an additional time" ([com.wingedsheep.sdk.scripting.AdditionalCastOrCopyTriggers]).
 */
class VeyranVoiceOfDualityScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.elementals(playerNumber: Int): Int {
            val playerId: EntityId = if (playerNumber == 1) player1Id else player2Id
            return state.getBattlefield(playerId).count {
                state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Elemental" } == true
            }
        }

        test("casting an instant or sorcery doubles Veyran's magecraft and another permanent's cast trigger") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Veyran, Voice of Duality")
                .withCardOnBattlefield(1, "Young Pyromancer")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            withClue("Divination + 2× Veyran magecraft + 2× Young Pyromancer") { game.state.stack.size shouldBe 5 }
            game.resolveStack()

            val veyran = game.findPermanent("Veyran, Voice of Duality")!!
            game.state.projectedState.getPower(veyran) shouldBe 4
            game.state.projectedState.getToughness(veyran) shouldBe 4
            game.elementals(1) shouldBe 2
        }

        test("copying a spell is doubled too") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Veyran, Voice of Duality")
                .withCardInHand(1, "Divination")
                .withCardInHand(1, "Fork")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Fork", "Divination").error shouldBe null
            game.resolveStack()

            // Divination cast (+2), Fork cast (+2), the copy (+2): 2/2 → 8/8.
            val veyran = game.findPermanent("Veyran, Voice of Duality")!!
            game.state.projectedState.getPower(veyran) shouldBe 8
        }

        test("an opponent casting a spell doesn't double their own permanent's trigger") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Veyran, Voice of Duality")
                .withCardOnBattlefield(2, "Young Pyromancer")
                .withCardInHand(2, "Divination")
                .withLandsOnBattlefield(2, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Divination").error shouldBe null
            withClue("Divination + one Young Pyromancer trigger") { game.state.stack.size shouldBe 2 }
            game.resolveStack()

            game.elementals(2) shouldBe 1
            game.state.projectedState.getPower(game.findPermanent("Veyran, Voice of Duality")!!) shouldBe 2
        }

        test("a creature spell isn't an instant or sorcery — nothing doubles") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Veyran, Voice of Duality")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.state.stack.size shouldBe 1
        }
    }
}
