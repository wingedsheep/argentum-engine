package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DashedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DashMechanicTest : FunSpec({

    val dashCreature = card("Dash Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental"
        power = 4
        toughness = 3
        dash = "{1}{R}"
    }

    // A dash creature whose dash cost itself contains {X} (no real printed dash card does this,
    // but the enumerator/payment path is a general primitive and must handle it like warp does).
    // It enters with X +1/+1 counters, so the chosen X must be applied to the dash cost.
    val dashXCreature = card("Dash X Creature") {
        manaCost = "{X}{G}{G}"
        typeLine = "Creature — Insect"
        power = 0
        toughness = 0
        dash = "{X}{G}"
        replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.XValue))
    }

    // Minimal blink (cf. Daydream): exile the targeted creature, then return it to the
    // battlefield — making it a new object (CR 400.7).
    val blinkSorcery = card("Dash Blink Test Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val creature = target("creature you control", Targets.CreatureYouControl)
            effect = Effects.Move(creature, Zone.EXILE)
                .then(Effects.Move(creature, Zone.BATTLEFIELD))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(dashCreature, dashXCreature, blinkSorcery))
        return driver
    }

    fun GameTestDriver.gotoMainPhase() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("dash creature can be cast for its dash cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("dashed creature enters battlefield with DashedComponent and has haste") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Dash Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<DashedComponent>() shouldBe true
        driver.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe true
    }

    test("dashed creature can attack the turn it's cast despite summoning sickness") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Dash Test Creature")!!
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(player, listOf(permanent), opponent)
        result.error shouldBe null
    }

    test("dashed creature is returned to hand at beginning of next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.findPermanent(player, "Dash Test Creature") shouldNotBe null

        // Advance to end step — delayed trigger should return it to hand
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(player, "Dash Test Creature") shouldBe null
        driver.findCardInHand(player, "Dash Test Creature") shouldNotBe null
    }

    test("dashed creature blinked before the end step is not returned to hand — new object, CR 603.7c") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val dashed = driver.findPermanent(player, "Dash Test Creature")
        dashed shouldNotBe null
        driver.state.getEntity(dashed!!)?.has<DashedComponent>() shouldBe true

        // Blink it — exile and return. The returned permanent is a new object (CR 400.7)
        // that dash's delayed return-to-hand trigger no longer tracks.
        val blinkId = driver.putCardInHand(player, "Dash Blink Test Sorcery")
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = blinkId,
                targets = listOf(ChosenTarget.Permanent(dashed)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        val returned = driver.findPermanent(player, "Dash Test Creature")
        returned shouldNotBe null
        driver.state.getEntity(returned!!)?.has<DashedComponent>() shouldBe false

        // The delayed trigger still fires at the end step but must not bounce the new object.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(player, "Dash Test Creature") shouldNotBe null
        driver.findCardInHand(player, "Dash Test Creature") shouldBe null
    }

    test("dashed creature killed before the end step stays in the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val dashed = driver.findPermanent(player, "Dash Test Creature")!!

        val transitionResult = ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = dashed,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(transitionResult.state)
        driver.findPermanent(player, "Dash Test Creature") shouldBe null

        // The delayed return-to-hand trigger still fires at the end step but the creature
        // already left the battlefield, so it must stay in the graveyard.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.getGraveyardCardNames(player).contains("Dash Test Creature") shouldBe true
        driver.findCardInHand(player, "Dash Test Creature") shouldBe null
    }

    test("creature cast for normal cost does not get DashedComponent or haste") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Dash Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<DashedComponent>() shouldBe false
        driver.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe false
    }

    // A dash card in hand can be cast two ways — its normal cost or its dash cost — and which to
    // use is the caster's choice (CR 118.9a). The action window must always surface both, even
    // when only one is payable, mirroring warp/morph.

    fun GameTestDriver.castOptionsFor(
        cardId: com.wingedsheep.sdk.model.EntityId,
    ): Pair<com.wingedsheep.engine.legalactions.LegalAction?, com.wingedsheep.engine.legalactions.LegalAction?> {
        val enumerator = LegalActionEnumerator.create(cardRegistry)
        val legalActions = enumerator.enumerate(state, activePlayer!!)
        val normalCast = legalActions.firstOrNull { action ->
            val cast = action.action as? CastSpell
            cast?.cardId == cardId && !cast.useAlternativeCost && !cast.castFaceDown
        }
        val dashCast = legalActions.firstOrNull { action ->
            val cast = action.action as? CastSpell
            cast?.cardId == cardId && cast.useAlternativeCost &&
                cast.alternativeCostType == AlternativeCostType.DASH
        }
        return normalCast to dashCast
    }

    test("dash card shows both normal and dash cast when only the dash cost is affordable") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        // Dash {1}{R} (2 mana, affordable) vs normal {3}{R}{R} (5 mana, not affordable).
        repeat(2) { driver.putLandOnBattlefield(player, "Mountain") }

        val (normalCast, dashCast) = driver.castOptionsFor(cardId)

        dashCast shouldNotBe null
        dashCast!!.affordable shouldBe true
        normalCast shouldNotBe null
        normalCast!!.affordable shouldBe false
    }

    test("dash card shows both normal and dash cast when neither cost is affordable") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")

        val (normalCast, dashCast) = driver.castOptionsFor(cardId)

        dashCast shouldNotBe null
        dashCast!!.affordable shouldBe false
        normalCast shouldNotBe null
        normalCast!!.affordable shouldBe false
    }

    test("dash card shows both normal and dash cast when both costs are affordable") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        // Five mana covers both the normal {3}{R}{R} and the dash {1}{R} cost.
        repeat(5) { driver.putLandOnBattlefield(player, "Mountain") }

        val (normalCast, dashCast) = driver.castOptionsFor(cardId)

        dashCast shouldNotBe null
        dashCast!!.affordable shouldBe true
        normalCast shouldNotBe null
        normalCast!!.affordable shouldBe true
    }

    test("dash cost containing X surfaces hasXCost so the client prompts for X") {
        // Regression: the dash enumeration path never set hasXCost, so a dash cost like
        // {X}{G} was cast with X = 0 (no X picker shown) — the same bug warp had before its fix.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash X Creature")
        driver.giveMana(player, Color.GREEN, 5)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val dashAction = enumerator.enumerate(driver.state, player).firstOrNull { action ->
            action.actionType == "CastWithDash" &&
                (action.action as? CastSpell)?.cardId == cardId
        }
        dashAction shouldNotBe null
        dashAction!!.hasXCost shouldBe true
        // Dash cost {X}{G}: 5 mana available, fixed {G} costs 1, so max X = (5 - 1) / 1 = 4.
        dashAction.maxAffordableX shouldBe 4
    }

    test("casting via dash applies chosen X to the dash cost and enters with X counters") {
        // The dash cost {X}{G} with X = 2 costs {2}{G}, and EntersWithDynamicCounters(XValue)
        // must read the same X so the creature enters with 2 +1/+1 counters.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash X Creature")
        driver.giveMana(player, Color.GREEN, 3) // {2}{G} for X = 2

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                xValue = 2,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Dash X Creature")
        permanent shouldNotBe null
        val counters = driver.state.getEntity(permanent!!)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 2
    }

    test("a creature dashed then returned to hand does not retain haste when cast again for its normal cost") {
        // Regression guard for the exact hazard ZoneMovementUtils' `.without<DashedComponent>()`
        // strip and MoveTrackedBattlefieldObjectExecutor's cleanup path ensure the same EntityId is
        // reused across a later fresh cast of the same physical card, so a stale DashedComponent
        // would silently grant haste it shouldn't.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val dashed = driver.findPermanent(player, "Dash Test Creature")
        dashed shouldNotBe null
        driver.state.getEntity(dashed!!)?.has<DashedComponent>() shouldBe true

        // Advance to end step and resolve the return-to-hand trigger.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.findPermanent(player, "Dash Test Creature") shouldBe null

        // Advance through the rest of turn 1, all of the opponent's turn 2, to player's turn 3 main phase.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast it again for its full normal cost — same underlying entity id.
        val recastCardId = driver.findCardInHand(player, "Dash Test Creature")!!
        recastCardId shouldBe cardId
        driver.giveMana(player, Color.RED, 5)
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = recastCardId,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        val recastPermanent = driver.findPermanent(player, "Dash Test Creature")
        recastPermanent shouldNotBe null
        driver.state.getEntity(recastPermanent!!)?.has<DashedComponent>() shouldBe false
        driver.state.projectedState.hasKeyword(recastPermanent, Keyword.HASTE) shouldBe false
    }

    test("casting a Dash creature from the graveyard for its dash cost is rejected") {
        // hasDashPermission (CastZoneResolver) gates dash to the hand zone (CR 702.109a — no
        // graveyard variant). The enumerator never offers this, so exercise the authoritative
        // handler directly with a hand-crafted action, mirroring how Ragavan's own land-exclusion
        // test checks the handler rejects it too, not just the enumerator.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInGraveyard(player, "Dash Test Creature")
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
        driver.getGraveyardCardNames(player).contains("Dash Test Creature") shouldBe true
        driver.findPermanent(player, "Dash Test Creature") shouldBe null
    }

    test("a forged DASH action from exile cannot fall back to a battlefield-granted alternative cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Jodah, Archmage Eternal")
        val cardId = driver.putCardInExile(player, "Dash Test Creature")
        driver.replaceState(
            driver.state.addMayPlayPermission(
                MayPlayPermission(
                    id = com.wingedsheep.sdk.model.EntityId.generate(),
                    cardIds = setOf(cardId),
                    controllerId = player,
                    timestamp = driver.state.timestamp
                )
            )
        )
        driver.giveMana(player, Color.WHITE)
        driver.giveMana(player, Color.BLUE)
        driver.giveMana(player, Color.BLACK)
        driver.giveMana(player, Color.RED)
        driver.giveMana(player, Color.GREEN)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DASH,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        result.isSuccess shouldBe false
        driver.state.getExile(player).contains(cardId) shouldBe true
        driver.findPermanent(player, "Dash Test Creature") shouldBe null
    }
})
