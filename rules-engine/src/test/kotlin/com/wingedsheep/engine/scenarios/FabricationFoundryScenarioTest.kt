package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.FabricationFoundry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Fabrication Foundry's reanimation ability — "{2}{W}, {T}, Exile one or more other artifacts you
 * control with total mana value X: Return target artifact card with mana value X or less from your
 * graveyard to the battlefield. Activate only as a sorcery."
 *
 * Exercises the new `Costs.ExilePermanents` variable-count cost end to end: X is the total mana
 * value of the exiled artifacts (CR 601.2b), and the graveyard target's "mana value X or less"
 * legality is measured against it at activation (CR 601.2c) and re-validated at resolution
 * (CR 608.2b). Drives the engine-direct path (a fully-formed action with the exile selection and
 * target both supplied), which the client's two-step pause flow converges to.
 */
class FabricationFoundryScenarioTest : FunSpec({

    // Test-local artifacts with known mana values (name → MV): MV0/MV1/MV2/MV3.
    val mv0 = card("Test MV0 Artifact") { manaCost = ""; typeLine = "Artifact" }
    val mv1 = card("Test MV1 Artifact") { manaCost = "{1}"; typeLine = "Artifact" }
    val mv2 = card("Test MV2 Artifact") { manaCost = "{2}"; typeLine = "Artifact" }
    val mv3 = card("Test MV3 Artifact") { manaCost = "{3}"; typeLine = "Artifact" }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FabricationFoundry, mv0, mv1, mv2, mv3))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        return driver
    }

    // The reanimation ability is the one that targets; the other is the restricted-mana ability.
    fun reanimateAbilityId() = FabricationFoundry.activatedAbilities.first { it.targetRequirements.isNotEmpty() }.id

    test("exiling a single mana-value-3 artifact sets X=3 and returns a mana-value-3 artifact card") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val toExile = driver.putPermanentOnBattlefield(p1, "Test MV3 Artifact")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV3 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(toExile))
            )
        )
        driver.bothPass()

        // The exiled artifact is in exile; the reanimated card is on the battlefield, out of the GY.
        driver.state.getZone(ZoneKey(p1, Zone.EXILE)).contains(toExile) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).contains(inGraveyard) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.GRAVEYARD)).contains(inGraveyard) shouldBe false
    }

    test("total mana value of multiple exiled artifacts sums into X (MV1 + MV2 = X3 returns MV3)") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val exile1 = driver.putPermanentOnBattlefield(p1, "Test MV1 Artifact")
        val exile2 = driver.putPermanentOnBattlefield(p1, "Test MV2 Artifact")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV3 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(exile1, exile2))
            )
        )
        driver.bothPass()

        driver.state.getZone(ZoneKey(p1, Zone.EXILE)).contains(exile1) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.EXILE)).contains(exile2) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).contains(inGraveyard) shouldBe true
    }

    test("a target whose mana value exceeds X is illegal (exile MV1 → X1 cannot return MV3)") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val toExile = driver.putPermanentOnBattlefield(p1, "Test MV1 Artifact")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV3 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(toExile))
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull()

        // Nothing moved: the artifact is still on the battlefield and the target still in the GY.
        driver.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).contains(toExile) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.GRAVEYARD)).contains(inGraveyard) shouldBe true
    }

    test("X can be 0 — exiling a mana-value-0 artifact returns a mana-value-0 artifact card") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val toExile = driver.putPermanentOnBattlefield(p1, "Test MV0 Artifact")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV0 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(toExile))
            )
        )
        driver.bothPass()

        driver.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).contains(inGraveyard) shouldBe true
    }

    test("X=0 cannot return a mana-value-1 artifact card") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val toExile = driver.putPermanentOnBattlefield(p1, "Test MV0 Artifact")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV1 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(toExile))
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull()
    }

    test("the exile cost must be OTHER artifacts — exiling Fabrication Foundry itself is illegal") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val foundry = driver.putPermanentOnBattlefield(p1, "Fabrication Foundry")
        val inGraveyard = driver.putCardInGraveyard(p1, "Test MV3 Artifact")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 3)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = foundry,
                abilityId = reanimateAbilityId(),
                targets = listOf(ChosenTarget.Card(inGraveyard, p1, Zone.GRAVEYARD)),
                costPayment = AdditionalCostPayment(exiledCards = listOf(foundry))
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull()

        // Foundry stays on the battlefield; nothing was reanimated.
        driver.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD)).contains(foundry) shouldBe true
        driver.state.getZone(ZoneKey(p1, Zone.GRAVEYARD)).contains(inGraveyard) shouldBe true
    }
})
