package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.SilverquillTheDisputant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Casualty N (CR 702.153): "As an additional cost to cast this spell, you may sacrifice a creature
 * with power N or greater. When you do, copy this spell and you may choose new targets for the
 * copy."
 *
 * Silverquill, the Disputant grants casualty 1 to each instant and sorcery spell its controller
 * casts (GrantKeywordToOwnSpells with keywordParameter = 1).
 */
class CasualtyTest : FunSpec({

    test("Granted Casualty: casting Shock and sacrificing a creature copies the spell") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        val fodder = driver.putCreatureOnBattlefield(caster, "Grizzly Bears") // power 2 >= 1
        driver.putLandOnBattlefield(caster, "Mountain")
        val shock = driver.putCardInHand(caster, "Shock")

        val castResult = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = shock,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                casualtyCreature = fodder
            )
        )
        castResult.isSuccess shouldBe true

        // The casualty creature was sacrificed to the graveyard.
        (fodder in driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(caster, Zone.GRAVEYARD))) shouldBe true

        // Pass priority; the casualty trigger resolves and pauses for copy retargeting.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        // Keep the same opponent target for the copy.
        driver.submitTargetSelection(caster, listOf(opponent)).isSuccess shouldBe true

        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        val copyTargets = driver.state.getEntity(copyId)!!.get<TargetsComponent>()
        copyTargets?.targets shouldBe listOf(ChosenTarget.Player(opponent))
    }

    test("Declining Casualty: Shock cast without casualtyCreature sacrifices nothing and adds no copy") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        val fodder = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putLandOnBattlefield(caster, "Mountain")
        val shock = driver.putCardInHand(caster, "Shock")

        driver.castSpell(caster, shock, listOf(opponent)).isSuccess shouldBe true

        // Creature stays on the battlefield; no copy on the stack.
        (fodder in driver.state.getBattlefield()) shouldBe true
        driver.state.stack.any { driver.state.getEntity(it)?.has<CopyOfComponent>() == true } shouldBe false
    }

    test("Casualty validation rejects a creature whose power is below the threshold") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        // A 0-power creature cannot pay casualty 1.
        val zeroPower = driver.putCreatureOnBattlefield(caster, "Ornithopter")
        driver.putLandOnBattlefield(caster, "Mountain")
        val shock = driver.putCardInHand(caster, "Shock")

        val result = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = shock,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                casualtyCreature = zeroPower
            )
        )
        result.isSuccess shouldBe false
    }

    test("Enumerator: Silverquill offers Casualty on an instant when an eligible creature exists") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putCardInHand(caster, "Shock")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.firstOrNull { it.actionType == "CastWithCasualty" } shouldNotBe null
    }

    test("Enumerator: a power-0 creature is excluded from the casualty sacrifice targets") {
        // Silverquill (power 4) itself qualifies, so the option is still offered — but a 0-power
        // Ornithopter must NOT appear among the valid sacrifice targets (CR 702.153 threshold).
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        val zeroPower = driver.putCreatureOnBattlefield(caster, "Ornithopter") // power 0
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putCardInHand(caster, "Shock")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val casualtyVariant = actions.firstOrNull { it.actionType == "CastWithCasualty" }
        casualtyVariant shouldNotBe null
        (zeroPower in (casualtyVariant!!.additionalCostInfo?.validSacrificeTargets ?: emptyList())) shouldBe false
    }

    test("Casualty not offered on a creature spell (filter is instant/sorcery only)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilverquillTheDisputant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Silverquill, the Disputant")
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putLandOnBattlefield(caster, "Forest")
        driver.putCardInHand(caster, "Grizzly Bears") // a creature spell

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.none { it.actionType == "CastWithCasualty" } shouldBe true
    }
})
