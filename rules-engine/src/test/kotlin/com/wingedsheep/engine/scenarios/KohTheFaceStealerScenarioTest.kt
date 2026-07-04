package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.KohTheFaceStealer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Koh, the Face Stealer — {4}{B}{B} Legendary Creature — Shapeshifter Spirit (TLA #107).
 *
 * "When Koh enters, exile up to one other target creature.
 *  Whenever another nontoken creature dies, you may exile it.
 *  Pay 1 life: Choose a creature card exiled with Koh.
 *  Koh has all activated and triggered abilities of the last chosen card."
 *
 * Verifies the [com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard] engine feature:
 * Koh gains both the activated and triggered abilities of the card it most recently chose from its
 * linked-exile pile, filled by the ETB exile and the dies trigger.
 */
class KohTheFaceStealerScenarioTest : FunSpec({

    // A creature whose only ability is "{T}: You gain 2 life" — the activated ability we expect Koh
    // to inherit once it chooses this card out of exile.
    val gainLifeBeast = card("Koh Test Beast") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 2
        toughness = 2
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val beastGainLifeAbilityId = gainLifeBeast.activatedAbilities[0].id

    // A creature with a triggered ability — "Whenever this creature attacks, you gain 3 life".
    val attackRaider = card("Koh Test Raider") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 2
        toughness = 2
        oracleText = "Whenever this creature attacks, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.Attacks
            effect = Effects.GainLife(3)
        }
    }

    // Plain removal to make a creature actually die (so Koh's dies trigger fires).
    val doom = card("Koh Test Doom") {
        manaCost = "{1}{B}"
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val creature = target("target creature", Targets.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    val kohPayLifeAbilityId = KohTheFaceStealer.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KohTheFaceStealer, gainLifeBeast, attackRaider, doom))
        return driver
    }

    /** Activate "Pay 1 life: Choose a creature card exiled with Koh" and choose [chosen]. */
    fun GameTestDriver.chooseExiled(me: com.wingedsheep.sdk.model.EntityId, koh: com.wingedsheep.sdk.model.EntityId, chosen: com.wingedsheep.sdk.model.EntityId) {
        submitSuccess(ActivateAbility(playerId = me, sourceId = koh, abilityId = kohPayLifeAbilityId))
        var guard = 0
        while (guard++ < 20) {
            val pd = pendingDecision
            when {
                pd is SelectCardsDecision -> submitCardSelection(me, listOf(chosen))
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("ETB exile a creature, choose it, and Koh gains its activated {T} ability") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putPermanentOnBattlefield(me, "Koh Test Beast")

        // Cast Koh so its ETB fires.
        val koh = driver.putCardInHand(me, "Koh, the Face Stealer")
        driver.giveColorlessMana(me, 4)
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, koh).isSuccess shouldBe true
        driver.bothPass() // resolve Koh → ETB goes on the stack and prompts a target

        driver.submitTargetSelection(me, listOf(beast))
        driver.bothPass() // resolve ETB → exile the beast linked to Koh

        driver.state.getZone(me, Zone.EXILE).contains(beast) shouldBe true
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(koh) shouldBe true

        // Koh was just cast; drop summoning sickness so a {T} ability isn't blocked by CR 302.6.
        driver.removeSummoningSickness(koh)

        // Before choosing, Koh does NOT have the beast's activated ability.
        driver.submit(ActivateAbility(playerId = me, sourceId = koh, abilityId = beastGainLifeAbilityId))
            .isSuccess shouldBe false

        // "Pay 1 life: Choose a creature card exiled with Koh."
        val lifeBeforeChoose = driver.getLifeTotal(me)
        driver.chooseExiled(me, koh, beast)
        driver.getLifeTotal(me) shouldBe lifeBeforeChoose - 1 // paid 1 life

        // Now Koh has "{T}: You gain 2 life".
        val lifeBefore = driver.getLifeTotal(me)
        driver.submit(ActivateAbility(playerId = me, sourceId = koh, abilityId = beastGainLifeAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 2
    }

    test("Koh gains the chosen card's triggered ability (attack trigger fires from Koh)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.player1
        val opp = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val raider = driver.putPermanentOnBattlefield(me, "Koh Test Raider")

        val koh = driver.putCardInHand(me, "Koh, the Face Stealer")
        driver.giveColorlessMana(me, 4)
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, koh).isSuccess shouldBe true
        driver.bothPass()
        driver.submitTargetSelection(me, listOf(raider))
        driver.bothPass()

        driver.state.getZone(me, Zone.EXILE).contains(raider) shouldBe true

        // Choose the raider so Koh gains "Whenever this creature attacks, you gain 3 life".
        driver.chooseExiled(me, koh, raider)

        // Attack with Koh; its inherited attack trigger should fire.
        driver.removeSummoningSickness(koh)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val lifeBefore = driver.getLifeTotal(me)
        driver.declareAttackers(me, listOf(koh), opp)
        // Resolve the attack trigger.
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }

    test("a dying nontoken creature may be exiled with Koh, then chosen for its ability") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Koh already in play (put directly — its ETB doesn't fire this way).
        val koh = driver.putPermanentOnBattlefield(me, "Koh, the Face Stealer")
        driver.removeSummoningSickness(koh)

        val beast = driver.putPermanentOnBattlefield(me, "Koh Test Beast")

        // Kill the beast so Koh's "whenever another nontoken creature dies" trigger fires.
        val bolt = driver.putCardInHand(me, "Koh Test Doom")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.castSpell(me, bolt, listOf(beast)).isSuccess shouldBe true

        // Resolve the removal and answer Koh's "you may exile it" with yes.
        var guard = 0
        while (guard++ < 20) {
            val pd = driver.pendingDecision
            when {
                pd is YesNoDecision -> driver.submitYesNo(me, true)
                pd is BatchYesNoDecision -> driver.submitBatchYesNo(me, choice = true, applyToAll = true)
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        // The beast is exiled with Koh (not in the graveyard).
        driver.state.getZone(me, Zone.EXILE).contains(beast) shouldBe true
        driver.state.getZone(me, Zone.GRAVEYARD).contains(beast) shouldBe false

        // Choose it and confirm Koh gained its {T} ability.
        driver.chooseExiled(me, koh, beast)
        val lifeBefore = driver.getLifeTotal(me)
        driver.submit(ActivateAbility(playerId = me, sourceId = koh, abilityId = beastGainLifeAbilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 2
    }
})
