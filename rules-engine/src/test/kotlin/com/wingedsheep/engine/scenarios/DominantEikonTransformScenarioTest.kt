package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CliveIfritsDominant
import com.wingedsheep.mtg.sets.definitions.fin.cards.JillShivasDominant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dominant / eikon transform (FIN §2): the front face's "Exile this, then return it transformed"
 * activated/triggered ability flips the legend into its Summon-Saga back face as a *new object*,
 * and the eikon Saga's final chapter exiles and returns it *front face up* — never the in-place
 * [com.wingedsheep.sdk.scripting.effects.TransformEffect]. Exercises
 * [com.wingedsheep.sdk.scripting.effects.ExileAndReturnTransformedEffect] in both directions.
 */
class DominantEikonTransformScenarioTest : FunSpec({

    val projector = StateProjector()

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    /**
     * Clear any decision that pauses the game by taking the minimal legal choice: decline "may"
     * yes/no prompts, and pick exactly each target requirement's minimum number of legal targets
     * (none for an optional "up to one", the sole legal target for a mandatory "target creature").
     */
    fun declineOptionalDecisions(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 12 && driver.isPaused) {
            when (val decision = driver.pendingDecision) {
                is YesNoDecision ->
                    driver.submitDecision(decision.playerId, YesNoResponse(decision.id, false))
                is ChooseTargetsDecision -> {
                    val chosen = decision.targetRequirements.associate { req ->
                        req.index to decision.legalTargets[req.index].orEmpty().take(req.minTargets)
                    }
                    driver.submitDecision(decision.playerId, TargetsResponse(decision.id, chosen))
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        declineOptionalDecisions(driver)
        resolveStack(driver)
        declineOptionalDecisions(driver)
    }

    fun newDriver(card: com.wingedsheep.sdk.model.CardDefinition, land: String): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(card))
        driver.initMirrorMatch(deck = Deck.of(land to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castAndResolve(driver: GameTestDriver, player: EntityId, name: String): EntityId {
        val spell = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.RED, 2)
        driver.giveMana(player, Color.BLUE, 2)
        driver.giveColorlessMana(player, 8)
        driver.castSpell(player, spell)
        driver.bothPass()
        resolveStack(driver)
        declineOptionalDecisions(driver) // decline the ETB "may"
        resolveStack(driver)
        return driver.findPermanent(player, name)!!
    }

    test("the front-face transform ability is sorcery-speed (CR: 'activate only as a sorcery')") {
        CliveIfritsDominant.activatedAbilities.first().timing shouldBe TimingRule.SorcerySpeed
        JillShivasDominant.activatedAbilities.first().timing shouldBe TimingRule.SorcerySpeed
    }

    test("Clive's activated ability exiles and returns it as the Ifrit Saga back — a new object") {
        val driver = newDriver(CliveIfritsDominant, "Mountain")
        val active = driver.activePlayer!!

        val clive = castAndResolve(driver, active, "Clive, Ifrit's Dominant")
        projector.project(driver.state).getPower(clive) shouldBe 5

        // {T} cost: bypass summoning sickness so the sorcery-speed ability can fire this turn.
        driver.removeSummoningSickness(clive)
        driver.giveMana(active, Color.RED, 2)
        driver.giveColorlessMana(active, 4)
        val abilityId = CliveIfritsDominant.activatedAbilities.first().id
        driver.submit(ActivateAbility(playerId = active, sourceId = clive, abilityId = abilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        declineOptionalDecisions(driver) // chapter I "fight up to one" — no other creature, none chosen
        resolveStack(driver)
        declineOptionalDecisions(driver)

        // Same entity id, now the back face: Ifrit, a 9/9 Enchantment-Creature Saga with a fresh
        // lore counter (it re-entered as a brand-new object, not flipped in place).
        val container = driver.state.getEntity(clive)!!
        container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name shouldBe
            "Ifrit, Warden of Inferno"
        val projected = projector.project(driver.state)
        projected.isCreature(clive) shouldBe true
        projected.hasType(clive, "Saga") shouldBe true
        projected.getPower(clive) shouldBe 9
        projected.getToughness(clive) shouldBe 9
        container.get<SagaComponent>() shouldNotBe null
        container.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
    }

    test("Ifrit's final chapter returns it front face up as Clive, rather than being sacrificed") {
        val driver = newDriver(CliveIfritsDominant, "Mountain")
        val active = driver.activePlayer!!

        val clive = castAndResolve(driver, active, "Clive, Ifrit's Dominant")
        driver.removeSummoningSickness(clive)
        driver.giveMana(active, Color.RED, 2)
        driver.giveColorlessMana(active, 4)
        val abilityId = CliveIfritsDominant.activatedAbilities.first().id
        driver.submit(ActivateAbility(playerId = active, sourceId = clive, abilityId = abilityId))
        driver.bothPass()
        declineOptionalDecisions(driver)
        resolveStack(driver)
        declineOptionalDecisions(driver)

        // Ifrit entered with lore 1. Accrue to lore 3 (chapter III "Brimstone" meets the
        // three-or-more-lore clause and flips Ifrit back to Clive, front face up).
        advanceToNextTurnMain(driver) // opp turn 2
        advanceToNextTurnMain(driver) // active turn 3 -> lore 2 (chapter II, condition false)
        driver.state.getEntity(clive)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
        advanceToNextTurnMain(driver) // opp turn 4
        advanceToNextTurnMain(driver) // active turn 5 -> lore 3 (chapter III -> return front)

        // Still on the battlefield (it returned itself instead of being sacrificed by CR 714.4),
        // now Clive again: front face, 5/5, no Saga machinery, no lore counter (a new object).
        val container = driver.state.getEntity(clive)!!
        container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name shouldBe
            "Clive, Ifrit's Dominant"
        projector.project(driver.state).getPower(clive) shouldBe 5
        container.get<SagaComponent>() shouldBe null
        (container.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0) shouldBe 0
    }

    test("Jill's activated ability returns it as the Shiva Saga back (4/5, fresh lore)") {
        val driver = newDriver(JillShivasDominant, "Island")
        val active = driver.activePlayer!!

        val jill = castAndResolve(driver, active, "Jill, Shiva's Dominant")
        driver.removeSummoningSickness(jill)
        driver.giveMana(active, Color.BLUE, 2)
        driver.giveColorlessMana(active, 3)
        val abilityId = JillShivasDominant.activatedAbilities.first().id
        driver.submit(ActivateAbility(playerId = active, sourceId = jill, abilityId = abilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        declineOptionalDecisions(driver)
        resolveStack(driver)
        declineOptionalDecisions(driver)

        val container = driver.state.getEntity(jill)!!
        container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name shouldBe
            "Shiva, Warden of Ice"
        val projected = projector.project(driver.state)
        projected.getPower(jill) shouldBe 4
        projected.getToughness(jill) shouldBe 5
        container.get<SagaComponent>() shouldNotBe null
        container.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
    }
})
