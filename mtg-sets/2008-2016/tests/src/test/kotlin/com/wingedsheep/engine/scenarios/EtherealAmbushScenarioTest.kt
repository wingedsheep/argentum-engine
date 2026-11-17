package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.EtherealAmbush
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Ethereal Ambush (FRF) — {3}{G}{U} Instant. "Manifest the top two cards of your library."
 *
 * The first card to manifest more than one card through `Patterns.Library.manifest(count)`: proves each
 * of the two cards becomes its own face-down 2/2 permanent and the rest of the library is untouched.
 */
class EtherealAmbushScenarioTest : FunSpec({

    test("manifests the top two cards as two separate face-down 2/2 creatures") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + EtherealAmbush)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val third = d.putCardOnTopOfLibrary(you, "Forest")
        val second = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val top = d.putCardOnTopOfLibrary(you, "Savannah Lions")
        val librarySize = d.state.getLibrary(you).size

        val spell = d.putCardInHand(you, "Ethereal Ambush")
        d.giveMana(you, Color.GREEN, 4)
        d.giveMana(you, Color.BLUE, 1)
        d.castSpell(you, spell).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.stack.isEmpty() shouldBe true
        for (manifested in listOf(top, second)) {
            d.getPermanents(you) shouldContain manifested
            val entity = d.state.getEntity(manifested)
            entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
            entity?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
            d.state.projectedState.isCreature(manifested) shouldBe true
            d.state.projectedState.getPower(manifested) shouldBe 2
            d.state.projectedState.getToughness(manifested) shouldBe 2
        }

        val library = d.state.getLibrary(you)
        library.size shouldBe librarySize - 2
        library.first() shouldBe third
        d.getPermanents(you) shouldNotContain third
    }
})
