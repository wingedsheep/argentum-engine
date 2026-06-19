package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ManifestedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Manifest Dread (CR 701.62 / 701.40) — Duskmourn: House of Horror.
 *
 * "Manifest dread" looks at the top two cards of the library, manifests one of them (the player's
 * choice) as a face-down 2/2 creature, and puts the other into the graveyard. A manifested
 * permanent can be turned face up by paying its mana cost — but only if the card representing it is
 * a creature card (CR 701.40b); a manifested non-creature can never be turned face up.
 *
 * Proven via the eponymous {1}{G} sorcery (Manifest Dread) and the enters-the-battlefield trigger
 * on Unsettling Twins (CR 701.62b).
 */
class ManifestDreadScenarioTest : FunSpec({

    // A bare sorcery that manifests the top card — exercises the plain Manifest mechanic (CR 701.40),
    // not the manifest-dread look-at-two variant.
    val manifestTester = card("Manifest Tester") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.manifest() }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + manifestTester)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    /** Cast Manifest Dread from hand, paying {1}{G} from the pool, and resolve until the pick pauses. */
    fun GameTestDriver.castManifestDread(you: EntityId) {
        val spell = putCardInHand(you, "Manifest Dread")
        giveMana(you, Color.GREEN, 2)
        castSpell(you, spell)
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("manifests the chosen card face down and puts the other into the graveyard") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack the top two cards: the creature on top, a land beneath it.
        val land = d.putCardOnTopOfLibrary(you, "Forest")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser") // {2}{G} 3/3 — now the top card

        d.castManifestDread(you)

        // Manifest dread pauses to choose which of the two looked-at cards to manifest.
        d.isPaused shouldBe true
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options.toSet() shouldBe setOf(creature, land)

        // Choose to manifest the creature; the land is the one not manifested.
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))

        // The creature is a face-down 2/2 on the battlefield; the land went to the graveyard.
        d.getPermanents(you) shouldContain creature
        val entity = d.state.getEntity(creature)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<ManifestedComponent>() shouldBe ManifestedComponent
        d.state.projectedState.getPower(creature) shouldBe 2
        d.state.projectedState.getToughness(creature) shouldBe 2
        d.state.projectedState.isCreature(creature) shouldBe true
        d.getGraveyard(you) shouldContain land
    }

    test("a manifested creature card can be turned face up for its mana cost") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardOnTopOfLibrary(you, "Forest")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        d.castManifestDread(you)
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))

        // Its turn-up cost is its real mana cost {2}{G} (CR 701.40b), not a morph cost.
        val turnUpData = d.state.getEntity(creature)?.get<FaceDownTurnUpComponent>()
        turnUpData.shouldNotBeNull()

        // Pay {2}{G} to turn it face up — it becomes the real 3/3 Centaur Courser.
        d.giveMana(you, Color.GREEN, 3)
        d.submit(TurnFaceUp(playerId = you, sourceId = creature, paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null

        val entity = d.state.getEntity(creature)
        entity?.get<FaceDownComponent>() shouldBe null
        entity?.get<ManifestedComponent>() shouldBe null
        d.getCardName(creature) shouldBe "Centaur Courser"
        d.state.projectedState.getPower(creature) shouldBe 3
    }

    test("a manifested non-creature card cannot be turned face up") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top two are a land (to manifest) and a creature (to bin).
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val land = d.putCardOnTopOfLibrary(you, "Forest") // now the top card
        d.castManifestDread(you)
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // Manifest the land — a face-down 2/2 with no way to turn face up.
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(land)))

        val entity = d.state.getEntity(land)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<ManifestedComponent>() shouldBe ManifestedComponent
        // No morph/turn-up data — a manifested non-creature can never be turned face up (CR 701.40b).
        entity?.get<FaceDownTurnUpComponent>() shouldBe null

        d.giveMana(you, Color.GREEN, 5)
        d.submit(TurnFaceUp(playerId = you, sourceId = land, paymentStrategy = PaymentStrategy.FromPool))
            .error shouldNotBe null
        d.state.getEntity(land)?.get<FaceDownComponent>() shouldBe FaceDownComponent
    }

    test("manifest dread with a single card in library manifests it and bins nothing") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Empty the library down to a single creature on top.
        d.replaceState(d.state.copy(zones = d.state.zones + (com.wingedsheep.engine.state.ZoneKey(you, com.wingedsheep.sdk.core.Zone.LIBRARY) to emptyList())))
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.castManifestDread(you)

        // Only one card was looked at. A forced 1-of-1 pick may still prompt; satisfy it if so.
        if (d.isPaused) {
            val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            pick.options shouldBe listOf(creature)
            d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        }

        // It is manifested; the only card in the graveyard is the resolved sorcery itself —
        // there was no second looked-at card to put into the graveyard.
        d.getPermanents(you) shouldContain creature
        d.state.getEntity(creature)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        d.getGraveyardCardNames(you) shouldBe listOf("Manifest Dread")
    }

    test("plain manifest puts the top card of the library face down") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val spell = d.putCardInHand(you, "Manifest Tester")
        d.giveMana(you, Color.GREEN, 1)
        d.castSpell(you, spell)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // No choice — the single top card is manifested directly as a face-down 2/2.
        d.getPermanents(you) shouldContain creature
        val entity = d.state.getEntity(creature)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<ManifestedComponent>() shouldBe ManifestedComponent
        d.state.projectedState.getPower(creature) shouldBe 2
    }

    test("an enters-the-battlefield trigger can manifest dread") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardOnTopOfLibrary(you, "Forest")
        val creature = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        // Unsettling Twins: "When this creature enters, manifest dread."
        d.putCardInHand(you, "Unsettling Twins")
        val twins = d.getHand(you).first { d.getCardName(it) == "Unsettling Twins" }
        d.giveMana(you, Color.WHITE, 4)
        d.castSpell(you, twins)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))

        d.state.getEntity(creature)?.get<ManifestedComponent>() shouldBe ManifestedComponent
        d.state.projectedState.getPower(creature) shouldBe 2
    }
})
