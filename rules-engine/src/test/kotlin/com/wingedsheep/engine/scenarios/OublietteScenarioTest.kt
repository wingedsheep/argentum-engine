package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Oubliette (Arabian Nights, {1}{B}{B} Enchantment).
 *
 * Oracle: "When this enchantment enters, target creature phases out until this enchantment leaves
 * the battlefield. Tap that creature as it phases in this way."
 *
 * Verifies the new linked-phasing primitive (PhaseOutUntilLeaves / PhaseInLinkedToSource): the
 * target phases out indefinitely, does NOT phase in at its controller's untap step, and phases back
 * in tapped when Oubliette leaves. Phasing is not a zone change, so phased-out checks use
 * `getBattlefield()` (which hides phased-out permanents), not the raw battlefield zone.
 */
class OublietteScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("phases out the target creature, then phases it back in tapped when Oubliette leaves") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // P1 casts Oubliette and its ETB trigger targets P2's Grizzly Bears.
        val oubliette = driver.putCardInHand(p1, "Oubliette")
        driver.giveMana(p1, Color.BLACK, 3) // {1}{B}{B}
        driver.castSpell(p1, oubliette)
        driver.bothPass() // resolve spell → Oubliette enters → ETB trigger
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(p1, listOf(bears))
        driver.bothPass() // resolve ETB → phase out

        // Phased out — hidden from the (phasing-aware) battlefield view.
        (bears in driver.state.getBattlefield()) shouldBe false

        // Destroy Oubliette with Disenchant; its leaves trigger phases the creature back in.
        val oublietteId = driver.findPermanent(p1, "Oubliette")!!
        val disenchant = driver.putCardInHand(p1, "Disenchant")
        driver.giveMana(p1, Color.WHITE, 2) // {1}{W}
        driver.castSpellWithTargets(p1, disenchant, listOf(ChosenTarget.Permanent(oublietteId)))
        driver.bothPass() // resolve Disenchant → Oubliette leaves
        driver.bothPass() // resolve Oubliette's leaves trigger → phase the creature in

        (bears in driver.state.getBattlefield()) shouldBe true // phased back in
        driver.isTapped(bears) shouldBe true // "Tap that creature as it phases in"
        driver.getController(bears) shouldBe p2 // phasing doesn't change control (Rule 702.26d)
        driver.findPermanent(p1, "Oubliette") shouldBe null
    }

    test("the phased-out creature does not phase in at its controller's untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val oubliette = driver.putCardInHand(p1, "Oubliette")
        driver.giveMana(p1, Color.BLACK, 3)
        driver.castSpell(p1, oubliette)
        driver.bothPass()
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(p1, listOf(bears))
        driver.bothPass()

        (bears in driver.state.getBattlefield()) shouldBe false

        // Advance into the creature controller's (P2's) turn — past their untap step.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe p2

        // Ordinary phasing would have phased it in at untap; linked phasing keeps it out while
        // Oubliette remains on the battlefield.
        (bears in driver.state.getBattlefield()) shouldBe false
        driver.findPermanent(p1, "Oubliette") shouldNotBe null
    }

    test("an Aura on the target phases out and back in attached (indirect phasing, Rule 702.26g)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20), startingLife = 20)
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // Enchant the bears with Holy Strength (+1/+2 Aura) before Oubliette removes them.
        val holyStrength = driver.putCardInHand(p1, "Holy Strength")
        driver.giveMana(p1, Color.WHITE, 1) // {W}
        driver.castSpell(p1, holyStrength, listOf(bears))
        driver.bothPass() // resolve → Aura attaches
        val aura = driver.findPermanent(p1, "Holy Strength")!!
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears

        // Oubliette the bears.
        val oubliette = driver.putCardInHand(p1, "Oubliette")
        driver.giveMana(p1, Color.BLACK, 3) // {1}{B}{B}
        driver.castSpell(p1, oubliette)
        driver.bothPass() // resolve spell → ETB trigger
        driver.submitTargetSelection(p1, listOf(bears))
        driver.bothPass() // resolve ETB → phase out

        // Both the creature and its Aura phase out — hidden from the battlefield view.
        (bears in driver.state.getBattlefield()) shouldBe false
        (aura in driver.state.getBattlefield()) shouldBe false

        // Destroy Oubliette; the creature and its Aura phase back in together, still attached.
        val oublietteId = driver.findPermanent(p1, "Oubliette")!!
        val disenchant = driver.putCardInHand(p1, "Disenchant")
        driver.giveMana(p1, Color.WHITE, 2) // {1}{W}
        driver.castSpellWithTargets(p1, disenchant, listOf(ChosenTarget.Permanent(oublietteId)))
        driver.bothPass() // resolve Disenchant → Oubliette leaves
        driver.bothPass() // resolve leaves trigger → phase in

        (bears in driver.state.getBattlefield()) shouldBe true
        (aura in driver.state.getBattlefield()) shouldBe true
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.isTapped(bears) shouldBe true // only the creature taps as it phases in
        driver.isTapped(aura) shouldBe false // attachments don't tap
    }
})
