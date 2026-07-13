package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.LodestoneNeedle
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Lodestone Needle // Guidestone Compass (LCI #62).
 *
 * Front — Lodestone Needle ({1}{U}, Artifact):
 *   Flash
 *   When this artifact enters, tap up to one target artifact or creature and put
 *   two stun counters on it.
 *   Craft with artifact {2}{U} (exactly one artifact material; CR 702.167).
 * Back — Guidestone Compass (Artifact):
 *   {1}, {T}: Target creature you control explores. Activate only as a sorcery.
 *
 * Covers:
 *  - Casting the front face fires the ETB trigger: the targeted opponent creature is
 *    tapped and gets two stun counters.
 *  - "Up to one target": declining the target resolves the trigger without effect.
 *  - Craft end-to-end: mana paid, artifact material exiled, source returns transformed
 *    as Guidestone Compass (back face); then the back-face ability makes a creature
 *    you control explore — a revealed land goes to hand (CR 701.44a).
 *  - Timing: the back-face explore ability is sorcery-only — rejected at upkeep.
 *  - Negative: a non-artifact creature is rejected as craft material.
 */
class LodestoneNeedleScenarioTest : FunSpec({

    // Simple artifact to use as craft material.
    val testTrinket = CardDefinition.artifact(
        name = "Test Trinket",
        manaCost = ManaCost.parse("{1}")
    )

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        // LodestoneNeedle is a catalog card, already included in TestCards.all.
        driver.registerCards(TestCards.all + listOf(testTrinket))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        return driver
    }

    // The craft ability is the front face's only activated ability.
    fun craftAbilityId() = LodestoneNeedle.activatedAbilities.single().id

    // The explore ability is the back face's only activated ability.
    fun exploreAbilityId() = LodestoneNeedle.backFace!!.activatedAbilities.single().id

    fun stunCounters(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    test("casting Lodestone Needle taps the targeted creature and puts two stun counters on it") {
        val driver = setup()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val victim = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val needle = driver.putCardInHand(p1, "Lodestone Needle")
        driver.giveMana(p1, Color.BLUE, 2)

        driver.castSpell(p1, needle).isSuccess shouldBe true
        driver.bothPass() // resolve the artifact spell; the ETB trigger asks for its target

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(victim)).error shouldBe null
        driver.bothPass() // resolve the ETB trigger

        driver.assertPermanentExists(p1, "Lodestone Needle")
        driver.isTapped(victim) shouldBe true
        stunCounters(driver, victim) shouldBe 2
    }

    test("up to one target: declining the target resolves the trigger without effect") {
        val driver = setup()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bystander = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val needle = driver.putCardInHand(p1, "Lodestone Needle")
        driver.giveMana(p1, Color.BLUE, 2)

        driver.castSpell(p1, needle).isSuccess shouldBe true
        driver.bothPass() // resolve the artifact spell; the ETB trigger asks for its target

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Choose no target ("up to one").
        driver.submitTargetSelection(p1, emptyList()).error shouldBe null
        driver.bothPass() // resolve the (targetless) trigger

        driver.assertPermanentExists(p1, "Lodestone Needle")
        driver.isTapped(bystander) shouldBe false
        stunCounters(driver, bystander) shouldBe 0
    }

    test("craft exiles the artifact material, returns as Guidestone Compass, and its ability makes a creature explore") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val needle = driver.putPermanentOnBattlefield(p1, "Lodestone Needle")
        val trinket = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val island = driver.putCardOnTopOfLibrary(p1, "Island")
        driver.giveMana(p1, Color.BLUE, 4) // {2}{U} craft + {1} explore

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = needle,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        driver.bothPass() // resolve the craft ability

        // Material exiled.
        driver.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContain trinket

        // Source returned to the battlefield as the back face.
        val container = driver.state.getEntity(needle)
        container.shouldNotBeNull()
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Guidestone Compass"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT)

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // {1}, {T}: Target creature you control explores — still in the precombat
        // main phase with an empty stack, so sorcery timing is satisfied.
        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = needle,
                abilityId = exploreAbilityId(),
                targets = listOf(entityIdToChosenTarget(driver.state, bears))
            )
        )
        driver.isTapped(needle) shouldBe true // {T} was part of the cost
        driver.bothPass() // resolve the explore

        // The revealed land goes to the hand; no +1/+1 counter (CR 701.44a).
        driver.getHand(p1) shouldContain island
        (driver.state.getEntity(bears)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }

    test("Guidestone Compass explore ability is rejected outside sorcery speed (upkeep)") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val needle = driver.putPermanentOnBattlefield(p1, "Lodestone Needle")
        val trinket = driver.putPermanentOnBattlefield(p1, "Test Trinket")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.BLUE, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = needle,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        driver.bothPass() // resolve the craft — Guidestone Compass on the battlefield
        driver.state.getEntity(needle)!!.get<CardComponent>()!!.name shouldBe "Guidestone Compass"

        // Advance to p1's own upkeep (turn 3): active player has priority, but it
        // is not a main phase — instant speed only.
        driver.passPriorityUntil(Step.UPKEEP)          // turn 2 upkeep (p2 active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)  // turn 2 main
        driver.passPriorityUntil(Step.UPKEEP)          // turn 3 upkeep (p1 active)
        driver.giveMana(p1, Color.BLUE, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = needle,
                abilityId = exploreAbilityId(),
                targets = listOf(entityIdToChosenTarget(driver.state, bears))
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull() shouldContain "sorcery"
    }

    test("rejects a non-artifact creature as craft material") {
        val driver = setup()
        val p1 = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val needle = driver.putPermanentOnBattlefield(p1, "Lodestone Needle")
        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.BLUE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = needle,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(bears))
            )
        )
        result.isSuccess shouldBe false
    }
})
