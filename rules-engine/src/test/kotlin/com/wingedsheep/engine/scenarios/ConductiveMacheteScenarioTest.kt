package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.ConductiveMachete
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Conductive Machete (DSK #244) — {4} Artifact — Equipment.
 *
 * "When this Equipment enters, manifest dread, then attach this Equipment to that creature."
 * Equipped creature gets +2/+1. Equip {4}.
 *
 * The ETB composes [com.wingedsheep.sdk.dsl.Patterns.Library.manifestDread] (which stores the
 * manifested creature under the pipeline collection "manifestDreadManifested") with
 * [com.wingedsheep.sdk.dsl.Effects.AttachEquipment] targeting that pipeline collection. We verify
 * the Machete attaches to the freshly-manifested 2/2 and that the +2/+1 buff applies (so a 2/2
 * manifest reads as 4/3).
 */
class ConductiveMacheteScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ConductiveMachete)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    test("ETB manifests dread and attaches to the manifested creature, granting +2/+1") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack the top two cards: a creature on top, a land beneath it.
        val land = d.putCardOnTopOfLibrary(you, "Forest")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser") // {2}{G} 3/3, now top

        val machete = d.putCardInHand(you, "Conductive Machete")
        d.giveMana(you, Color.GREEN, 4)
        d.castSpell(you, machete)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // Manifest dread pauses to choose which looked-at card to manifest.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The creature is a manifested 2/2 on the battlefield.
        val manifested = d.state.getEntity(creature)
        manifested?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST

        // The Machete (find it on the battlefield) is attached to that creature.
        val macheteEntity = d.getPermanents(you).first { d.getCardName(it) == "Conductive Machete" }
        val attachedTo = d.state.getEntity(macheteEntity)?.get<AttachedToComponent>()
        attachedTo.shouldNotBeNull()
        attachedTo.targetId shouldBe creature

        // +2/+1 on a 2/2 manifest = 4/3.
        d.state.projectedState.getPower(creature) shouldBe 4
        d.state.projectedState.getToughness(creature) shouldBe 3
    }
})
