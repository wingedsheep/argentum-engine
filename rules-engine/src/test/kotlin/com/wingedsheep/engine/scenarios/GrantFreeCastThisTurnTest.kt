package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastThisTurnEffect
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for the state-held free-cast permission
 * ([com.wingedsheep.sdk.scripting.effects.GrantFreeCastThisTurnEffect]): the one-shot "the next
 * matching spell you cast this turn can be cast without paying its mana cost" rider behind World War
 * Hulk chapter I, and the whole-turn "until end of turn, you may cast spells from your hand without
 * paying their mana costs" behind Chandra, Flame's Catalyst's −8 (the tests at the end).
 *
 * Sibling of [MakeNextSpellUncounterableTest]: a pending rider held on the game state, matched
 * against the controller's next cast and then consumed. The properties that distinguish it from the
 * battlefield static [MayCastWithoutPayingManaCost] — it outlives its source, it isn't gated on
 * being the turn's first spell, and it's spent by the cast rather than by the discount — are what
 * this file pins down. The card-level filter behaviour lives in [WorldWarHulkScenarioTest].
 */
class GrantFreeCastThisTurnTest : FunSpec({

    // A sorcery: by the time the free cast happens the source is in the graveyard, so a passing
    // test proves the permission doesn't depend on its source being on the battlefield.
    val freeCastRitual = card("Free Cast Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GrantNextSpellFreeCast()
        }
    }

    // The same rider, narrowed to creature spells, to exercise the filter.
    val creatureFreeCastRitual = card("Creature Free Cast Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GrantNextSpellFreeCast(GameObjectFilter.Creature)
        }
    }

    // A Zaffai-style once-per-turn battlefield permission, to prove a rider-funded free cast
    // doesn't burn it.
    val onceEachTurnFreeCaster = card("Test Once-Per-Turn Free Caster") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        staticAbility {
            ability = MayCastWithoutPayingManaCost(controllerOnly = true, oncePerTurn = true)
        }
    }

    // An {X} spell, to pin CR 107.3b: a free cast pays no mana cost at all, so no value is chosen
    // for X and X is 0. "You gain X life" makes that directly observable.
    val xLifeRitual = card("Test X Life Ritual") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GainLife(DynamicAmount.XValue)
        }
    }

    // A creature spell with a *mandatory* non-mana additional cost. "Without paying its mana cost"
    // (CR 118.9) is an alternative cost and replaces only the mana cost, so this sacrifice is still
    // owed on a free cast.
    val sacrificeGolem = card("Test Blood Golem") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Golem"
        power = 4
        toughness = 4
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
    }

    // The whole-turn form, scoped to hand casts like Chandra, Flame's Catalyst's −8.
    val handFreeCastRitual = card("Hand Free Cast Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.GrantFreeCastThisTurn(fromZones = setOf(Zone.HAND))
        }
    }

    // The one-shot form scoped to hand casts, to pin that a cast from elsewhere doesn't spend it.
    val nextHandFreeCastRitual = card("Next Hand Free Cast Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = GrantFreeCastThisTurnEffect(fromZones = setOf(Zone.HAND))
        }
    }

    // A graveyard-castable spell (flashback {G}) — a cast that isn't from the hand.
    val flashbackLife = card("Test Flashback Life") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.flashback("{G}"))
        spell { effect = Effects.GainLife(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                freeCastRitual, creatureFreeCastRitual, onceEachTurnFreeCaster,
                xLifeRitual, sacrificeGolem, handFreeCastRitual, nextHandFreeCastRitual, flashbackLife
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast [ritualName] and let it resolve, leaving a pending rider. */
    fun GameTestDriver.castRitual(player: EntityId, ritualName: String) {
        giveMana(player, Color.GREEN, 1)
        val ritual = putCardInHand(player, ritualName)
        castSpell(player, ritual).outcome shouldBe Outcome.Done
        bothPass()
        state.pendingFreeCastSpells shouldNotBe emptyList<Any>()
    }

    test("the next spell can be cast without paying its mana cost, with the source in the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Free Cast Ritual")
        withClue("the rider's source has already gone to the graveyard") {
            driver.findPermanent(player, "Free Cast Ritual") shouldBe null
        }

        // Force of Nature is {2}{G}{G}{G}; the pool is empty, so this is free or it fails.
        val fatty = driver.putCardInHand(player, "Force of Nature")
        driver.submit(
            CastSpell(player, fatty, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(player, "Force of Nature") shouldNotBe null
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }

    test("the rider is consumed by the next matching cast even when the free cast isn't taken") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castRitual(player, "Free Cast Ritual")

        driver.giveMana(player, Color.GREEN, 2)
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, bears).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("'the next spell you cast' names a spell — paying full price still spends it") {
            driver.state.pendingFreeCastSpells.shouldBeEmpty()
            costCalculator.hasFreeCastPermission(driver.state, player) shouldBe false
        }
    }

    test("an unused rider is cleared at the start of the next turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Free Cast Ritual")
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }

    test("a creature-only rider ignores a noncreature spell and pays for the next creature spell") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castRitual(player, "Creature Free Cast Ritual")

        withClue("a noncreature spell is outside the filter, so no free cast is offered") {
            costCalculator.hasFreeCastPermission(
                driver.state,
                player,
                driver.cardRegistry.requireCard("Lightning Bolt")
            ) shouldBe false
        }

        driver.giveMana(player, Color.RED, 1)
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()
        withClue("casting it doesn't consume the creature-only rider either") {
            driver.state.pendingFreeCastSpells.size shouldBe 1
        }

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.submit(
            CastSpell(player, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }

    test("a rider-funded free cast doesn't burn a once-per-turn battlefield permission") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val zaffai = driver.putPermanentOnBattlefield(player, "Test Once-Per-Turn Free Caster")
        driver.castRitual(player, "Free Cast Ritual")

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.submit(
            CastSpell(player, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("the rider paid for this cast, so the once-per-turn source keeps its use") {
            driver.state.getEntity(zaffai)!!
                .has<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldBe false
        }

        // And the once-per-turn permission is still usable afterwards.
        val second = driver.putCardInHand(player, "Force of Nature")
        driver.submit(
            CastSpell(player, second, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Force of Nature") shouldNotBe null
    }

    test("the rider belongs to its controller: the opponent is neither offered it nor spends it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castRitual(player, "Free Cast Ritual")

        withClue("the opponent gets no permission out of someone else's rider") {
            costCalculator.hasFreeCastPermission(
                driver.state,
                opponent,
                driver.cardRegistry.requireCard("Grizzly Bears")
            ) shouldBe false
        }

        // The rider is cleared at the next turn boundary, so the opponent's only window to cast
        // while it is live is at instant speed during the granting turn.
        driver.passPriority(player)
        driver.priorityPlayer shouldBe opponent
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(player))).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("a spell cast by the other player is not 'the next spell you cast'") {
            driver.state.pendingFreeCastSpells.size shouldBe 1
            driver.state.pendingFreeCastSpells.single().controllerId shouldBe player
        }
    }

    test("the enumerated free cast has no X — a free cast pays no mana cost, so X is 0 (CR 107.3b)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Free Cast Ritual")
        val xSpell = driver.putCardInHand(player, "Test X Life Ritual")

        // Go through the legal-action enumerator, not a hand-built action: this is *how* CR 107.3b
        // is enforced here. The offered free cast carries no X and is not flagged `hasXCost`, so
        // no legal action can ever declare one. (The handler does not independently reject a
        // hand-built `CastSpell(useWithoutPayingManaCost = true, xValue = n)`: with n generic mana
        // available it charges the mana and resolves with X = n. That is pre-existing plumbing
        // shared with every free-cast path — Weftwalking, Omniscience, emblems — and unreachable
        // through the enumerated actions the server offers; see this branch's PR body.)
        val freeCast = driver.legalActions(player)
            .filter { it.actionType == "CastWithoutPayingManaCost" }
            .single { (it.action as CastSpell).cardId == xSpell }
        withClue("the free-cast variant never prompts for X") {
            freeCast.hasXCost shouldBe false
            (freeCast.action as CastSpell).xValue shouldBe null
        }

        driver.submit(freeCast.action).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("'you gain X life' gained 0 life") {
            driver.getLifeTotal(player) shouldBe 20
        }
    }

    test("a free cast still pays a mandatory additional cost (CR 118.9 — only the mana cost is replaced)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val fodder = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.castRitual(player, "Free Cast Ritual")
        val golem = driver.putCardInHand(player, "Test Blood Golem")

        withClue("the free cast doesn't waive the sacrifice") {
            driver.submit(
                CastSpell(player, golem, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
            ).outcome shouldNotBe Outcome.Done
            driver.state.pendingFreeCastSpells.size shouldBe 1
        }

        driver.submit(
            CastSpell(
                player,
                golem,
                useWithoutPayingManaCost = true,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("the mana cost was free, the sacrifice was not") {
            driver.findPermanent(player, "Test Blood Golem") shouldNotBe null
            driver.findPermanent(player, "Grizzly Bears") shouldBe null
            driver.getGraveyard(player) shouldContain fodder
            driver.state.pendingFreeCastSpells.shouldBeEmpty()
        }
    }
    // --- Whole turn (nextSpellOnly = false): Chandra, Flame's Catalyst −8 -----------------------

    fun GameTestDriver.castFree(player: EntityId, cardId: EntityId) {
        submit(
            CastSpell(player, cardId, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("whole turn: every spell cast from hand this turn may be free, and nothing is consumed") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Hand Free Cast Ritual")

        driver.castFree(player, driver.putCardInHand(player, "Force of Nature"))
        driver.castFree(player, driver.putCardInHand(player, "Grizzly Bears"))
        driver.castFree(player, driver.putCardInHand(player, "Force of Nature"))

        driver.getCreatures(player).count { driver.getCardName(it) == "Force of Nature" } shouldBe 2
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
        withClue("the permission is still there for the rest of the turn") {
            driver.state.pendingFreeCastSpells.single().nextSpellOnly shouldBe false
        }
    }

    test("whole turn: the permission ends with the turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Hand Free Cast Ritual")
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }

    test("whole turn, from hand: a spell cast from the graveyard still pays its cost") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castRitual(player, "Hand Free Cast Ritual")
        val flashback = driver.putCardInGraveyard(player, "Test Flashback Life")
        val flashbackDef = driver.cardRegistry.requireCard("Test Flashback Life")

        costCalculator.hasFreeCastPermission(driver.state, player, flashbackDef, Zone.HAND) shouldBe true
        withClue("'from your hand' — the graveyard cast is not covered") {
            costCalculator.hasFreeCastPermission(driver.state, player, flashbackDef, Zone.GRAVEYARD) shouldBe false
            driver.legalActions(player).none {
                it.actionType == "CastWithoutPayingManaCost" && (it.action as CastSpell).cardId == flashback
            } shouldBe true
        }
    }

    test("whole turn: timing is unchanged — a sorcery-speed spell can't be cast free in combat") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Hand Free Cast Ritual")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val freeCasts = driver.legalActions(player)
            .filter { it.actionType == "CastWithoutPayingManaCost" }
            .map { (it.action as CastSpell).cardId }
        withClue("the instant may be cast free in combat; the creature must wait for a main phase") {
            freeCasts shouldContain bolt
            freeCasts.contains(bears) shouldBe false
        }
    }

    test("whole turn: the opponent gets nothing from it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castRitual(player, "Hand Free Cast Ritual")
        costCalculator.hasFreeCastPermission(
            driver.state, opponent, driver.cardRegistry.requireCard("Lightning Bolt"), Zone.HAND
        ) shouldBe false
    }

    test("a hand-scoped next-spell rider isn't spent by a cast from the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.castRitual(player, "Next Hand Free Cast Ritual")
        val flashback = driver.putCardInGraveyard(player, "Test Flashback Life")
        driver.giveMana(player, Color.GREEN, 1)
        val flashbackCast = driver.legalActions(player)
            .single { it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == flashback }
        driver.submit(flashbackCast.action).outcome shouldBe Outcome.Done
        driver.bothPass()

        withClue("the flashback cast came from the graveyard, so it isn't the rider's spell") {
            driver.state.pendingFreeCastSpells.size shouldBe 1
        }

        driver.castFree(player, driver.putCardInHand(player, "Grizzly Bears"))
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }
})
