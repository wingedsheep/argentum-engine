package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Substrate tests for The Ring mechanic (CR 701.54): the "the Ring tempts you" effect, the
 * Ring-bearer designation, the legendary + can't-be-blocked-by-greater-power static abilities,
 * the "Whenever the Ring tempts you" trigger, and the tempt-count-gated combat ability.
 */
class TheRingScenarioTest : FunSpec({

    val projector = StateProjector()

    // {0} sorcery: "The Ring tempts you."
    val RingTempter = card("Ring Tempter") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "The Ring tempts you."
        spell { effect = Effects.TheRingTemptsYou() }
    }

    // Enchantment: "Whenever the Ring tempts you, you gain 2 life."
    val RingWatcher = card("Ring Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Whenever the Ring tempts you, you gain 2 life."
        triggeredAbility {
            trigger = Triggers.RingTemptsYou
            effect = Effects.GainLife(2)
        }
    }

    val Bear = CardDefinition.creature("Ring Bear", ManaCost.parse("{2}"), emptySet(), 2, 2)
    val BigOgre = CardDefinition.creature("Big Ogre", ManaCost.parse("{3}"), emptySet(), 3, 3)
    val SmallGoblin = CardDefinition.creature("Small Goblin", ManaCost.parse("{1}"), emptySet(), 1, 1)
    // Power 2 (can legally block a 3-power Ring-bearer), toughness 4 (survives 3 combat damage so
    // it's still around to be sacrificed at end of combat).
    val StoutBlocker = CardDefinition.creature("Stout Blocker", ManaCost.parse("{3}"), emptySet(), 2, 4)

    // {0} sorcery: "Gain control of target creature until end of turn." — Threaten in miniature,
    // used to exercise CR 701.54a (Ring-bearer designation ends when another player gains control).
    val ThreatenTest = card("Threaten Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Gain control of target creature until end of turn."
        spell {
            val t = target("creature", Targets.Creature)
            effect = Effects.GainControl(t, Duration.EndOfTurn)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(RingTempter, RingWatcher, Bear, BigOgre, SmallGoblin, StoutBlocker, ThreatenTest)
        )
        return driver
    }

    // Cast "Ring Tempter" and resolve it, choosing [bearerId] as the Ring-bearer when prompted.
    fun GameTestDriver.tempt(player: EntityId, bearerId: EntityId?) {
        val cardId = putCardInHand(player, "Ring Tempter")
        castSpell(player, cardId)
        bothPass()
        val decision = pendingDecision
        if (decision is SelectCardsDecision && bearerId != null) {
            submitDecision(player, CardsSelectedResponse(decision.id, listOf(bearerId)))
        }
    }

    test("tempting designates a Ring-bearer, grants the emblem, and makes the bearer legendary") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")
        driver.tempt(active, bear)

        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 1
        driver.state.getEntity(bear)?.get<RingBearerComponent>()?.ownerId shouldBe active
        projector.project(driver.state).isLegendary(bear) shouldBe true
    }

    test("the Ring-bearer is surfaced to the client as isRingBearer; a non-bearer is not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")
        val ogre = driver.putCreatureOnBattlefield(active, "Big Ogre")
        driver.tempt(active, bear)

        val view = ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = active)
        view.cards[bear]?.isRingBearer shouldBe true
        view.cards[ogre]?.isRingBearer shouldBe false

        // CR 701.54c makes the bearer legendary, and the client type line has to say so — the
        // emblem's supertype grant lives only in the projection, never on the base CardComponent.
        view.cards[bear]?.typeLine shouldBe "Legendary Creature"
        view.cards[bear]?.legendaryByEffect shouldBe true
        view.cards[ogre]?.typeLine shouldBe "Creature"
        view.cards[ogre]?.legendaryByEffect shouldBe false
    }

    test("CR 701.54a: another player gaining control ends the Ring-bearer designation permanently") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val owner = driver.activePlayer!!                  // bear's owner — tempts themselves first
        val thief = driver.getOpponent(owner)              // will steal the bear on their next turn
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(owner, "Ring Bear")
        driver.tempt(owner, bear)
        driver.state.getEntity(bear)?.get<RingBearerComponent>()?.ownerId shouldBe owner

        // Hand the turn to the would-be thief and arrive at their main phase. UNTAP /
        // CLEANUP don't get priority, so we wait on DRAW (which does) instead.
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer != thief) driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.activePlayer shouldBe thief
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)

        // Thief steals the bear with a Threaten-style spell.
        val threaten = driver.putCardInHand(thief, "Threaten Test")
        driver.castSpell(thief, threaten, listOf(bear))
        driver.bothPass()
        projector.project(driver.state).getController(bear) shouldBe thief

        // The designation ended the moment control changed (CR 701.54a) — the component
        // is stripped eagerly so that when control reverts the bear stays not-the-bearer.
        driver.state.getEntity(bear)?.get<RingBearerComponent>() shouldBe null

        // Advance to the owner's next draw step so the EndOfTurn floating control effect has
        // expired and control has reverted.
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer != owner) driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.activePlayer shouldBe owner

        projector.project(driver.state).getController(bear) shouldBe owner
        driver.state.getEntity(bear)?.get<RingBearerComponent>() shouldBe null
        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<RingBearerComponent>()?.ownerId == owner
        } shouldBe false
    }

    test("tempting again moves the Ring-bearer designation to the new creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val first = driver.putCreatureOnBattlefield(active, "Ring Bear")
        val second = driver.putCreatureOnBattlefield(active, "Small Goblin")

        driver.tempt(active, first)
        driver.tempt(active, second)

        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 2
        driver.state.getEntity(first)?.get<RingBearerComponent>() shouldBe null
        driver.state.getEntity(second)?.get<RingBearerComponent>()?.ownerId shouldBe active
    }

    test("Ring-bearer can't be blocked by creatures with greater power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear") // 2/2
        driver.removeSummoningSickness(bear)
        val ogre = driver.putCreatureOnBattlefield(opponent, "Big Ogre")       // 3/3 — greater power
        val goblin = driver.putCreatureOnBattlefield(opponent, "Small Goblin") // 1/1 — lesser power

        driver.tempt(active, bear)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(bear), opponent)
        driver.bothPass()

        // The 3/3 can't block the 2/2 Ring-bearer.
        val illegal = driver.declareBlockers(opponent, mapOf(ogre to listOf(bear)))
        illegal.error shouldNotBe null

        // The 1/1 can.
        val legal = driver.declareBlockers(opponent, mapOf(goblin to listOf(bear)))
        legal.error shouldBe null
    }

    test("'Whenever the Ring tempts you' triggers") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(active, "Ring Watcher")
        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear")
        val lifeBefore = driver.getLifeTotal(active)

        driver.tempt(active, bear)
        driver.bothPass() // resolve the "Whenever the Ring tempts you" triggered ability

        driver.getLifeTotal(active) shouldBe lifeBefore + 2
    }

    test("tempting with no creatures still tempts: count increments and the trigger fires (CR 701.54a/d)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The player controls an enchantment but no creatures, so there's no Ring-bearer to choose.
        driver.putPermanentOnBattlefield(active, "Ring Watcher")
        val lifeBefore = driver.getLifeTotal(active)

        // No creature to pick — tempt(...) resolves without a SelectCardsDecision.
        driver.tempt(active, bearerId = null)
        driver.bothPass() // resolve the "Whenever the Ring tempts you" triggered ability

        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 1
        driver.state.getBattlefield().any {
            driver.state.getEntity(it)?.get<RingBearerComponent>() != null
        } shouldBe false
        driver.getLifeTotal(active) shouldBe lifeBefore + 2
    }

    test("tempted four times: Ring-bearer's combat damage to a player drains each opponent 3") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(active, "Ring Bear") // 2/2
        driver.removeSummoningSickness(bear)
        repeat(4) { driver.tempt(active, bear) }
        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 4

        val opponentLifeBefore = driver.getLifeTotal(opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(bear), opponent)

        // Drive combat to its end. At tempt count 4 the bearer also has the ≥2 "draw a card, then
        // discard a card" attack trigger, so auto-resolve any intervening discard / combat-damage
        // decisions until the combat phase ends.
        var guard = 0
        while (driver.currentStep != Step.POSTCOMBAT_MAIN && guard++ < 40) {
            when (val decision = driver.pendingDecision) {
                is SelectCardsDecision ->
                    driver.submitDecision(
                        decision.playerId,
                        CardsSelectedResponse(decision.id, decision.options.take(maxOf(1, decision.minSelections)))
                    )
                is CombatResolutionDecision -> driver.confirmCombatDamage()
                else -> driver.bothPass()
            }
        }

        // 2 combat damage + 3 from the Ring's fourth ability.
        driver.getLifeTotal(opponent) shouldBe opponentLifeBefore - 5
    }

    test("tempted three times: blocker of the Ring-bearer is sacrificed at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bearer = driver.putCreatureOnBattlefield(active, "Big Ogre") // 3/3
        driver.removeSummoningSickness(bearer)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Stout Blocker") // 2/4, survives combat
        repeat(3) { driver.tempt(active, bearer) }
        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 3

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(bearer), opponent)

        // The ≥2 "draw a card, then discard a card" attack trigger resolves first — auto-resolve its
        // discard decision and advance to the declare-blockers step before declaring blockers.
        var g1 = 0
        while (driver.currentStep != Step.DECLARE_BLOCKERS && g1++ < 20) {
            when (val decision = driver.pendingDecision) {
                is SelectCardsDecision ->
                    driver.submitDecision(
                        decision.playerId,
                        CardsSelectedResponse(decision.id, decision.options.take(maxOf(1, decision.minSelections)))
                    )
                else -> driver.bothPass()
            }
        }
        driver.declareBlockers(opponent, mapOf(blocker to listOf(bearer)))

        // Resolve the becomes-blocked trigger, combat damage, and the end-of-combat sacrifice.
        var g2 = 0
        while (driver.currentStep != Step.POSTCOMBAT_MAIN && g2++ < 40) {
            when (val decision = driver.pendingDecision) {
                is SelectCardsDecision ->
                    driver.submitDecision(
                        decision.playerId,
                        CardsSelectedResponse(decision.id, decision.options.take(maxOf(1, decision.minSelections)))
                    )
                is CombatResolutionDecision -> driver.confirmCombatDamage()
                else -> driver.bothPass()
            }
        }

        // The blocker survived combat damage but is sacrificed at end of combat (CR 701.54c, ≥3).
        driver.findPermanent(opponent, "Stout Blocker") shouldBe null
    }
})
