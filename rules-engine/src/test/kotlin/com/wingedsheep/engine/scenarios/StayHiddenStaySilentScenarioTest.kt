package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stay Hidden, Stay Silent (DSK #74) — {1}{U} Enchantment — Aura.
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * {4}{U}{U}: Shuffle enchanted creature into its owner's library, then manifest dread. Activate
 * only as a sorcery.
 *
 * Exercises:
 *  - the ETB trigger tapping the enchanted creature;
 *  - the "doesn't untap" static keeping it tapped through its controller's untap step;
 *  - the sorcery-speed activated ability shuffling the enchanted creature away (the Aura falls off
 *    as an SBA) and then manifesting dread (a new face-down 2/2 appears).
 */
class StayHiddenStaySilentScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("ETB trigger taps the enchanted creature and it stays tapped through untap") {
        val d = driver()
        val me = d.player1

        val creature = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(creature)
        d.isTapped(creature) shouldBe false

        val aura = d.putCardInHand(me, "Stay Hidden, Stay Silent")
        d.giveMana(me, Color.BLUE, 2)
        d.castSpell(me, aura, listOf(creature))
        // Resolve the aura, then its ETB tap trigger.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.isTapped(creature) shouldBe true
        d.findPermanent(me, "Stay Hidden, Stay Silent") shouldNotBe null

        // Advance to this player's next untap step; the creature must NOT untap.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe me
        d.isTapped(creature) shouldBe true
    }

    test("activated ability shuffles the enchanted creature away and manifests dread") {
        val d = driver()
        val me = d.player1

        val creature = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(creature)

        val aura = d.putCardInHand(me, "Stay Hidden, Stay Silent")
        d.giveMana(me, Color.BLUE, 2)
        d.castSpell(me, aura, listOf(creature))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()
        val auraPermanent = d.findPermanent(me, "Stay Hidden, Stay Silent")!!

        // A manifested permanent is a face-down 2/2 regardless of its underlying card type, so
        // count by FaceDownComponent over all permanents (not getCreatures, which reads the base
        // type line and would miss a manifested land).
        val faceDownBefore = d.getPermanents(me).count {
            d.state.getEntity(it)?.has<FaceDownComponent>() == true
        }

        val abilityId = d.cardRegistry.requireCard("Stay Hidden, Stay Silent").activatedAbilities[0].id
        d.giveMana(me, Color.BLUE, 6)
        d.submit(
            ActivateAbility(playerId = me, sourceId = auraPermanent, abilityId = abilityId)
        )
        // Resolve: shuffle the creature into library, then manifest dread (pick which to manifest).
        var guard = 0
        while (guard++ < 30) {
            val pd = d.pendingDecision
            if (pd is SelectCardsDecision) {
                d.submitDecision(me, CardsSelectedResponse(pd.id, listOf(pd.options.first())))
            } else if (d.state.stack.isNotEmpty()) {
                d.bothPass()
            } else break
        }

        // The Grizzly Bears (as itself) is gone from the battlefield, and the Aura fell off.
        d.findPermanent(me, "Grizzly Bears") shouldBe null
        d.findPermanent(me, "Stay Hidden, Stay Silent") shouldBe null

        // Manifest dread put a new face-down 2/2 onto the battlefield.
        val faceDownAfter = d.getPermanents(me).count {
            d.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
        faceDownAfter shouldBe faceDownBefore + 1
    }
})
