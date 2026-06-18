package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Oltec Matterweaver (BIG #3).
 *
 * {2}{W} Creature — Human Artificer 2/4.
 *   Whenever you cast a creature spell, choose one —
 *   • Create a 1/1 colorless Gnome artifact creature token.
 *   • Create a token that's a copy of target artifact token you control.
 */
class OltecMatterweaverScenarioTest : ScenarioTestBase() {

    init {
        test("choosing mode 1 creates a 1/1 Gnome artifact creature token on a creature cast") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Oltec Matterweaver")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null

            // The "whenever you cast a creature spell" trigger goes on the stack above the spell.
            // Resolve it first → mode choice.
            game.resolveStack()
            val modeDecision = game.getPendingDecision() as ChooseOptionDecision
            withClue("two modes are offered") {
                modeDecision.options.size shouldBe 2
            }
            game.submitDecision(OptionChosenResponse(modeDecision.id, 0))
            game.resolveStack()

            val gnome = game.findPermanent("Gnome Token")
            withClue("a Gnome token is created") {
                (gnome != null) shouldBe true
            }
            val gnomeCard = game.state.getEntity(gnome!!)!!.get<CardComponent>()!!
            withClue("Gnome token is a 1/1 artifact creature") {
                gnomeCard.typeLine.cardTypes shouldContainType CardType.ARTIFACT
                gnomeCard.typeLine.cardTypes shouldContainType CardType.CREATURE
                gnomeCard.typeLine.subtypes shouldContainSubtype Subtype("Gnome")
            }
        }

        test("choosing mode 2 copies a target artifact token you control") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Oltec Matterweaver")
                .withCardOnBattlefield(1, "Treasure", isToken = true)
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            val modeDecision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(modeDecision.id, 1))

            // Mode 2 targets an artifact token; supply the Treasure as the target.
            val targetDecision = game.getPendingDecision() as ChooseTargetsDecision
            val treasure = game.findPermanent("Treasure")!!
            game.selectTargets(listOf(treasure))
            game.resolveStack()

            withClue("a second Treasure token (the copy) now exists") {
                game.findPermanents("Treasure").size shouldBe 2
            }
        }
    }

    private infix fun Set<CardType>.shouldContainType(type: CardType) {
        (type in this) shouldBe true
    }

    private infix fun Set<Subtype>.shouldContainSubtype(subtype: Subtype) {
        (subtype in this) shouldBe true
    }
}
