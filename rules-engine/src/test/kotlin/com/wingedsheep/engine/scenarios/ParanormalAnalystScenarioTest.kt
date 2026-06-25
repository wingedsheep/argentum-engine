package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Paranormal Analyst (DSK) — "Whenever you manifest dread, put a card you put into your graveyard
 * this way into your hand."
 *
 * Exercises the manifest-dread trigger (CR 701.60): manifesting dread emits a `ManifestedDreadEvent`
 * carrying the card put into the graveyard this way, which the engine threads onto the resolving
 * trigger's pipeline (`TRIGGER_CAPTURED_COLLECTION`). Paranormal Analyst's payoff moves that card
 * from the graveyard back to its controller's hand.
 *
 * Per CR 701.60b the trigger fires even when the library held fewer than two cards (nothing to put
 * in the graveyard); the payoff is then a safe no-op.
 */
class ParanormalAnalystScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    /** Cast Manifest Dread from hand, paying {1}{G} from the pool, and resolve until the pick pauses. */
    fun GameTestDriver.castManifestDread(you: EntityId) {
        val spell = putCardInHand(you, "Manifest Dread")
        giveMana(you, Color.GREEN, 2)
        castSpell(you, spell)
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("returns the binned card from the graveyard to hand when you manifest dread") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(you, "Paranormal Analyst")

        // Stack the top two: creature on top, a land beneath it.
        val land = d.putCardOnTopOfLibrary(you, "Forest")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.castManifestDread(you)

        // Manifest dread pauses to choose which of the two looked-at cards to manifest.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options.toSet() shouldBe setOf(creature, land)

        // Manifest the creature; the land is the one put into the graveyard this way.
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))

        // The manifest-dread trigger now resolves: move the binned land to hand.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getPermanents(you) shouldContain creature
        d.getHand(you) shouldContain land
        d.getGraveyard(you) shouldNotContain land
    }

    test("trigger fires safely and returns nothing when the library has too few cards to bin") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(you, "Paranormal Analyst")

        // Empty the library down to a single creature on top — manifest dread looks at one card,
        // manifests it, and has nothing to put into the graveyard.
        d.replaceState(d.state.copy(zones = d.state.zones + (ZoneKey(you, Zone.LIBRARY) to emptyList())))
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val handBefore = d.getHand(you).toSet()

        d.castManifestDread(you)

        // A forced 1-of-1 pick may still prompt; satisfy it if so.
        if (d.isPaused) {
            val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        }
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The creature is manifested; the trigger fired but found nothing to return — hand is
        // unchanged (only the resolved sorcery sits in the graveyard).
        d.getPermanents(you) shouldContain creature
        d.getHand(you).toSet() shouldBe handBefore
        d.getGraveyardCardNames(you) shouldBe listOf("Manifest Dread")
    }
})
