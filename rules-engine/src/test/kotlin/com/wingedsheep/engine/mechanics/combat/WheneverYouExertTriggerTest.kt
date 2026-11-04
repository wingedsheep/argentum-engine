package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Whenever you exert a creature" — `Triggers.you.exerts(filter)` over `EventPattern.ExertedEvent`
 * (Vizier of the True, Resolute Survivors).
 *
 * - CR 701.43a: exerting is the player's choice, so "you" is the player who exerted it, and every
 *   exert counts — an optional attack cost (CR 701.43d) or an activated ability's exert cost.
 * - Ruling (2017-04-18): such abilities trigger when you exert the source or any other creature.
 * - CR 701.43b: a permanent can be exerted even if already exerted; that is still an exert.
 */
class WheneverYouExertTriggerTest : FunSpec({

    val watcher = card("Exert Watcher") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        staticAbility { ability = ExertAsItAttacks() }
        triggeredAbility {
            trigger = Triggers.you.exerts(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }

    val exerter = card("Exert Fodder") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
        staticAbility { ability = ExertAsItAttacks() }
        activatedAbility {
            cost = Costs.Exert
            effect = Effects.DrawCards(1)
        }
    }

    val exertLand = card("Exert Arena") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Exert
            effect = Effects.DrawCards(1)
        }
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + watcher + exerter + exertLand)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        return Triple(d, active, d.getOpponent(active))
    }

    fun activateExert(d: GameTestDriver, player: EntityId, source: EntityId, abilityCard: com.wingedsheep.sdk.model.CardDefinition) {
        d.submitSuccess(ActivateAbility(playerId = player, sourceId = source, abilityId = abilityCard.activatedAbilities[0].id))
    }

    test("exerting another creature as it attacks fires the trigger") {
        val (d, active, defender) = setup()
        d.putCreatureOnBattlefield(active, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        d.removeSummoningSickness(fodder)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(fodder), defender).outcome.shouldBeInstanceOf<Outcome.Paused>()
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val life = d.getLifeTotal(active)
        d.submitCardSelection(active, listOf(fodder))
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 1
    }

    test("exerting the source itself as it attacks fires it") {
        val (d, active, defender) = setup()
        val self = d.putCreatureOnBattlefield(active, "Exert Watcher")
        d.removeSummoningSickness(self)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(self), defender)
        val life = d.getLifeTotal(active)
        d.submitCardSelection(active, listOf(self))
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 1
    }

    test("exerting two creatures as they attack fires once for each") {
        val (d, active, defender) = setup()
        val self = d.putCreatureOnBattlefield(active, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        listOf(self, fodder).forEach(d::removeSummoningSickness)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(self, fodder), defender)
        val life = d.getLifeTotal(active)
        d.submitCardSelection(active, listOf(self, fodder))
        d.state.stack.size shouldBe 2
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 2
    }

    test("declining the attack exert fires nothing") {
        val (d, active, defender) = setup()
        d.putCreatureOnBattlefield(active, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        d.removeSummoningSickness(fodder)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(fodder), defender)
        d.submitCardSelection(active, emptyList())
        d.state.stack.isEmpty() shouldBe true
    }

    test("paying an activated ability's exert cost fires it, unlike the linked attack trigger") {
        val (d, active, _) = setup()
        d.putCreatureOnBattlefield(active, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        val life = d.getLifeTotal(active)
        activateExert(d, active, fodder, exerter)
        d.state.stack.size shouldBe 2 // the draw ability, and the exert trigger on top of it
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 1
    }

    test("re-exerting an already-exerted creature is still an exert (CR 701.43b)") {
        val (d, active, _) = setup()
        d.putCreatureOnBattlefield(active, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        d.replaceState(d.state.updateEntity(fodder) { it.with(ExertedComponent) })
        val life = d.getLifeTotal(active)
        activateExert(d, active, fodder, exerter)
        d.bothPass()
        d.getLifeTotal(active) shouldBe life + 1
    }

    test("exerting a noncreature permanent doesn't match a creature filter") {
        val (d, active, _) = setup()
        d.putCreatureOnBattlefield(active, "Exert Watcher")
        val land = d.putPermanentOnBattlefield(active, "Exert Arena")
        activateExert(d, active, land, exertLand)
        d.state.stack.size shouldBe 1 // only the draw ability
    }

    test("an opponent exerting their creature doesn't fire your trigger") {
        val (d, active, defender) = setup()
        d.putCreatureOnBattlefield(defender, "Exert Watcher")
        val fodder = d.putCreatureOnBattlefield(active, "Exert Fodder")
        activateExert(d, active, fodder, exerter)
        d.state.stack.size shouldBe 1 // only the draw ability
    }
})
