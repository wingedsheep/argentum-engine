package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.InvertedIceberg
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Inverted Iceberg // Iceberg Titan (LCI #60).
 *
 * Front — Inverted Iceberg ({1}{U}, Artifact):
 *   When this artifact enters, mill a card, then draw a card.
 *   Craft with artifact {4}{U}{U} (exactly one artifact material; CR 702.167).
 * Back — Iceberg Titan (Artifact Creature — Golem, 6/6):
 *   Whenever this creature attacks, you may tap or untap target artifact or creature.
 *
 * Covers:
 *  - Casting the front face fires the ETB trigger: top card milled to the graveyard,
 *    then the next card drawn (mill happens before the draw).
 *  - Craft end-to-end with a battlefield artifact material: mana paid, material exiled,
 *    source returns transformed as a 6/6 Iceberg Titan (back face).
 *  - Craft with an artifact card from the graveyard as material (CR 702.167b).
 *  - Back-face attack trigger: target chosen at declare-attackers, may-clause accepted,
 *    "Tap it" mode taps the targeted opponent creature.
 *  - Negative: a non-artifact (creature) material is rejected.
 *  - Negative: two materials are rejected ("Craft with artifact" = exactly one).
 */
class InvertedIcebergScenarioTest : FunSpec({

    // Simple artifact to use as craft material.
    val testTrinket = CardDefinition.artifact(
        name = "Test Trinket",
        manaCost = ManaCost.parse("{1}")
    )

    val projector = StateProjector()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        // InvertedIceberg is a catalog card, already included in TestCards.all.
        driver.registerCards(TestCards.all + listOf(testTrinket))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        return driver
    }

    // The craft ability is the front face's only activated ability.
    fun craftAbilityId() = InvertedIceberg.activatedAbilities.single().id

    test("casting Inverted Iceberg mills the top card, then draws the next one") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Seed the library: second call ends up on top, so `milled` is the top card
        // (goes to the graveyard) and `drawn` sits beneath it (drawn afterwards).
        val drawn = driver.putCardOnTopOfLibrary(p1, "Island")
        val milled = driver.putCardOnTopOfLibrary(p1, "Island")

        val iceberg = driver.putCardInHand(p1, "Inverted Iceberg")
        val handBefore = driver.getHandSize(p1)
        val graveyardBefore = driver.getGraveyard(p1).size

        driver.giveMana(p1, Color.BLUE, 2)
        driver.castSpell(p1, iceberg).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the artifact spell; ETB trigger goes on the stack
        driver.bothPass() // resolve the ETB trigger: mill a card, then draw a card

        driver.assertPermanentExists(p1, "Inverted Iceberg")
        driver.getGraveyard(p1).size shouldBe graveyardBefore + 1
        driver.getGraveyard(p1) shouldContain milled
        driver.getHand(p1) shouldContain drawn
        // Net: -1 (cast) +1 (draw).
        driver.getHandSize(p1) shouldBe handBefore
    }

    test("craft with a battlefield artifact exiles it and returns Inverted Iceberg as a 6/6 Iceberg Titan") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val iceberg = driver.putPermanentOnBattlefield(p1, "Inverted Iceberg")
        val trinket = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        driver.giveMana(p1, Color.BLUE, 6)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = iceberg,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        driver.bothPass() // resolve the craft ability

        // Material exiled.
        driver.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContain trinket

        // Source returned to the battlefield as the back face.
        val container = driver.state.getEntity(iceberg)
        container.shouldNotBeNull()
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Iceberg Titan"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        val projected = projector.project(driver.state)
        projected.getPower(iceberg) shouldBe 6
        projected.getToughness(iceberg) shouldBe 6
    }

    test("craft accepts an artifact card from the graveyard as material (CR 702.167b)") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val iceberg = driver.putPermanentOnBattlefield(p1, "Inverted Iceberg")
        val trinket = driver.putCardInGraveyard(p1, "Test Trinket")
        driver.giveMana(p1, Color.BLUE, 6)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = iceberg,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        driver.bothPass()

        driver.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContain trinket
        driver.state.getEntity(iceberg)!!.get<CardComponent>()!!.name shouldBe "Iceberg Titan"
    }

    test("Iceberg Titan attack trigger may tap target artifact or creature") {
        val driver = setup()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val iceberg = driver.putPermanentOnBattlefield(p1, "Inverted Iceberg")
        val trinket = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        val victim = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.giveMana(p1, Color.BLUE, 6)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = iceberg,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        driver.bothPass() // resolve the craft; Iceberg Titan is on the battlefield

        // The crafted Titan entered this turn — clear summoning sickness so it can attack.
        driver.removeSummoningSickness(iceberg)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val attackResult = driver.declareAttackers(p1, listOf(iceberg), p2)
        attackResult.error shouldBe null

        // Announce the target, then answer consent at resolution.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(victim))

        driver.bothPass() // resolve to optional consent
        driver.submitYesNo(p1, true).error shouldBe null // then the tap-or-untap choice

        val modeDecision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val tapIndex = modeDecision.options.indexOfFirst { it.startsWith("Tap") }
        (tapIndex >= 0) shouldBe true
        driver.submitDecision(p1, OptionChosenResponse(modeDecision.id, tapIndex))

        driver.isTapped(victim) shouldBe true
    }

    test("rejects a non-artifact creature as craft material") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val iceberg = driver.putPermanentOnBattlefield(p1, "Inverted Iceberg")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.BLUE, 6)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = iceberg,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(bears))
            )
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("rejects two materials — 'Craft with artifact' takes exactly one") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val iceberg = driver.putPermanentOnBattlefield(p1, "Inverted Iceberg")
        val trinketA = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        val trinketB = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        driver.giveMana(p1, Color.BLUE, 6)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = iceberg,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinketA, trinketB))
            )
        )
        result.outcome shouldNotBe Outcome.Done
    }
})
