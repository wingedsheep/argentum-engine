package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.RaidingSchemes
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Conspire (CR 702.78): "As you cast this spell, you may tap two untapped creatures you
 * control that share a color with it. When you do, copy it and you may choose new targets
 * for the copy." Raiding Schemes grants Conspire to all noncreature spells its controller
 * casts.
 */
class ConspireTest : FunSpec({

    test("Granted Conspire: casting Lightning Bolt with two red creatures tapped copies the spell") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaidingSchemes))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Raiding Schemes grants Conspire to noncreature spells.
        driver.putPermanentOnBattlefield(caster, "Raiding Schemes")

        // Two untapped red creatures the caster controls.
        val goblin1 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        val goblin2 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")

        repeat(1) { driver.putLandOnBattlefield(caster, "Mountain") }
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        val castResult = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                conspiredCreatures = listOf(goblin1, goblin2)
            )
        )
        castResult.isSuccess shouldBe true

        // Conspire creatures are tapped by cost payment.
        driver.state.getEntity(goblin1)!!.has<TappedComponent>() shouldBe true
        driver.state.getEntity(goblin2)!!.has<TappedComponent>() shouldBe true

        // Pass priority; the Conspire trigger resolves and pauses for copy retargeting.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        // Choose the caster as the copy's new target; the copy resolves before the original.
        driver.submitTargetSelection(caster, listOf(caster)).isSuccess shouldBe true

        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        val copyTargets = driver.state.getEntity(copyId)!!.get<TargetsComponent>()
        copyTargets?.targets shouldBe listOf(ChosenTarget.Player(caster))
    }

    test("Declining Conspire: bolt cast without conspiredCreatures leaves creatures untapped and adds no copy") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaidingSchemes))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Raiding Schemes")
        val goblin1 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        val goblin2 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        // Goblins stay untapped because conspiredCreatures was empty.
        driver.state.getEntity(goblin1)!!.has<TappedComponent>() shouldBe false
        driver.state.getEntity(goblin2)!!.has<TappedComponent>() shouldBe false

        // No copy should exist on the stack.
        val hasCopy = driver.state.stack.any { id ->
            driver.state.getEntity(id)?.has<CopyOfComponent>() == true
        }
        hasCopy shouldBe false
    }

    test("Conspire validation rejects creatures that share no color with the spell") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaidingSchemes))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Raiding Schemes")
        val redGoblin = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        // Centaur Courser is green — shares no color with a red Lightning Bolt.
        val greenCentaur = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        val result = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                conspiredCreatures = listOf(redGoblin, greenCentaur)
            )
        )
        result.isSuccess shouldBe false
    }

    test("Enumerator: Raiding Schemes grants Conspire to a noncreature spell when eligible creatures exist") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaidingSchemes))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Raiding Schemes")
        driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putCardInHand(caster, "Lightning Bolt")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val conspireVariant = actions.firstOrNull { it.actionType == "CastWithConspire" }
        conspireVariant shouldNotBe null
    }

    test("Enumerator: Conspire not offered when only one color-matching creature is available") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaidingSchemes))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Raiding Schemes")
        driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        driver.putCreatureOnBattlefield(caster, "Centaur Courser") // Green, doesn't share
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putCardInHand(caster, "Lightning Bolt")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val conspireVariant = actions.firstOrNull { it.actionType == "CastWithConspire" }
        conspireVariant shouldBe null
    }
})
