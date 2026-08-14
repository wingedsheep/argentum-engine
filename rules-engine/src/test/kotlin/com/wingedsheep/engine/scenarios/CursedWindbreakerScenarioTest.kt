package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.CursedWindbreaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cursed Windbreaker (DSK #47) — {2}{U} Artifact — Equipment.
 *
 * "When this Equipment enters, manifest dread, then attach this Equipment to that creature."
 * Equipped creature has flying. Equip {3}.
 *
 * The ETB composes [com.wingedsheep.sdk.dsl.Patterns.Library.manifestDread] (storing the manifested
 * creature under the pipeline collection "manifestDreadManifested") with
 * [com.wingedsheep.sdk.dsl.Effects.AttachEquipment] targeting that pipeline collection. We verify the
 * Windbreaker attaches to the freshly-manifested 2/2 and grants it flying.
 */
class CursedWindbreakerScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + CursedWindbreaker)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    test("ETB manifests dread and attaches to the manifested creature, granting flying") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack the top two cards: a creature on top, a land beneath it.
        d.putCardOnTopOfLibrary(you, "Island")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser") // {2}{G} 3/3, now top

        val windbreaker = d.putCardInHand(you, "Cursed Windbreaker")
        d.giveMana(you, Color.BLUE, 3)
        d.castSpell(you, windbreaker)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // Manifest dread pauses to choose which looked-at card to manifest.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The creature is a manifested 2/2 on the battlefield.
        val manifested = d.state.getEntity(creature)
        manifested?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST

        // The Windbreaker (find it on the battlefield) is attached to that creature.
        val windbreakerEntity = d.getPermanents(you).first { d.getCardName(it) == "Cursed Windbreaker" }
        val attachedTo = d.state.getEntity(windbreakerEntity)?.get<AttachedToComponent>()
        attachedTo.shouldNotBeNull()
        attachedTo.targetId shouldBe creature

        // The equipped, manifested creature has flying.
        d.state.projectedState.hasKeyword(creature, Keyword.FLYING) shouldBe true
    }
})
