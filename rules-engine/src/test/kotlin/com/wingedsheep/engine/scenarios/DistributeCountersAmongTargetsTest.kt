package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Distribute N counters among targets" — the controller chooses the division, each target getting
 * at least one (CR 601.2d). A triggered ability announces it as it goes on the stack (CR 603.3d), and
 * the announced shares are honored verbatim: a target that became illegal loses its share and is not
 * re-divided (CR 608.2b). An activated ability announces it as part of the activation (CR 601.2d via
 * CR 602.2b): the legal action asks the client for it and the validator checks it. A spell, or an
 * activation that leaves it out, divides at resolution.
 */
class DistributeCountersAmongTargetsTest : FunSpec({
    val seeder = card("Counter Seeder Probe") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            targets(TargetFilter.Creature, count = 3, minCount = 1)
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
        }
    }
    val chainSeeder = card("Counter Chain Seeder Probe") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            targets(TargetFilter.Creature, count = 3, minCount = 1)
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 3) then Effects.GainLife(2)
        }
    }
    val spray = card("Counter Spray Probe") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            targets(TargetFilter.Creature, count = 2, minCount = 1)
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
        }
    }

    // Ajani, Sleeper Agent's −3 shape: a divided step followed by a per-target rider.
    val totem = card("Counter Totem Probe") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            targets(TargetFilter.Creature, count = 3, minCount = 0)
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 3) then Effects.GainLife(2)
        }
    }

    fun newGame(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(seeder, chainSeeder, spray, totem))
        d.initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast [name], let it resolve, and answer its enters trigger's targets. */
    fun GameTestDriver.castAndTarget(name: String, targets: List<EntityId>) {
        val creature = putCardInHand(player1, name)
        giveMana(player1, Color.GREEN, 1)
        castSpell(player1, creature).error shouldBe null
        bothPass().error shouldBe null
        submitTargetSelection(player1, targets).error shouldBe null
    }

    test("a trigger announces an uneven split before it goes on the stack") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.castAndTarget("Counter Seeder Probe", listOf(courser, lions))

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.totalAmount shouldBe 3
        decision.minPerTarget shouldBe 1
        decision.context.phase shouldBe DecisionPhase.CASTING
        d.stackSize shouldBe 0

        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 1, lions to 2))).error shouldBe null
        d.stackSize shouldBe 1
        val onStack = d.state.getEntity(d.getTopOfStack()!!)!!.get<TriggeredAbilityOnStackComponent>()!!
        onStack.damageDistribution shouldBe mapOf(courser to 1, lions to 2)

        d.bothPass().error shouldBe null
        d.plusOnes(courser) shouldBe 1
        d.plusOnes(lions) shouldBe 2
    }

    test("every target must receive at least one — leaving one out of the division is rejected") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.castAndTarget("Counter Seeder Probe", listOf(courser, lions))
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()

        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 3))).error shouldNotBe null
        d.stackSize shouldBe 0
    }

    test("a target removed in response loses its share; the survivor keeps exactly what it was assigned") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.castAndTarget("Counter Seeder Probe", listOf(courser, lions))
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 1, lions to 2))).error shouldBe null

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(lions)).error shouldBe null
        d.bothPass().error shouldBe null // Bolt resolves, Lions die
        d.bothPass().error shouldBe null // the trigger resolves

        (lions in d.state.getBattlefield()) shouldBe false
        d.plusOnes(courser) shouldBe 1
    }

    test("a single target takes the whole pool with nothing to divide") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        d.castAndTarget("Counter Seeder Probe", listOf(courser))
        d.pendingDecision shouldBe null
        d.bothPass().error shouldBe null

        d.plusOnes(courser) shouldBe 3
    }

    test("the divided step of a sequence is announced too, and the rest of the sequence still runs") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val life = d.getLifeTotal(d.player1)

        d.castAndTarget("Counter Chain Seeder Probe", listOf(courser, lions))
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 2, lions to 1))).error shouldBe null
        d.bothPass().error shouldBe null

        d.plusOnes(courser) shouldBe 2
        d.plusOnes(lions) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe life + 2
    }

    test("a spell with no announced division asks for it at resolution") {
        val d = newGame()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        val sorcery = d.putCardInHand(d.player1, "Counter Spray Probe")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, sorcery, listOf(courser, lions)).error shouldBe null
        d.bothPass().error shouldBe null

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.totalAmount shouldBe 3
        decision.minPerTarget shouldBe 1
        decision.targets.toSet() shouldBe setOf(courser, lions)
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 1, lions to 2))).error shouldBe null

        d.plusOnes(courser) shouldBe 1
        d.plusOnes(lions) shouldBe 2
    }

    fun GameTestDriver.activateTotem(totemId: EntityId, targets: List<EntityId>, division: Map<EntityId, Int>?) =
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = totemId,
                abilityId = totem.activatedAbilities.single().id,
                targets = targets.map { ChosenTarget.Permanent(it) },
                damageDistribution = division,
            )
        )

    test("an activated ability's legal action asks for the counter division, with its total, floor and unit") {
        val d = newGame()
        val totemId = d.putPermanentOnBattlefield(d.player1, "Counter Totem Probe")
        d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        val action = d.legalActions(d.player1).single { (it.action as? ActivateAbility)?.sourceId == totemId }
        action.requiresDamageDistribution shouldBe true
        action.totalDamageToDistribute shouldBe 3
        action.minDamagePerTarget shouldBe 1
        action.distributionUnit shouldBe "+1/+1 counter"
    }

    test("an activated ability carries the division announced at activation and honors it verbatim") {
        val d = newGame()
        val totemId = d.putPermanentOnBattlefield(d.player1, "Counter Totem Probe")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val life = d.getLifeTotal(d.player1)

        d.activateTotem(totemId, listOf(courser, lions), mapOf(courser to 2, lions to 1)).error shouldBe null
        d.pendingDecision shouldBe null
        val onStack = d.state.getEntity(d.getTopOfStack()!!)!!.get<ActivatedAbilityOnStackComponent>()!!
        onStack.damageDistribution shouldBe mapOf(courser to 2, lions to 1)

        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        d.plusOnes(courser) shouldBe 2
        d.plusOnes(lions) shouldBe 1
        d.getLifeTotal(d.player1) shouldBe life + 2
    }

    test("an activation's division must spend exactly the pool and give every target its floor") {
        val d = newGame()
        val totemId = d.putPermanentOnBattlefield(d.player1, "Counter Totem Probe")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.activateTotem(totemId, listOf(courser, lions), mapOf(courser to 1, lions to 1)).error shouldNotBe null
        d.activateTotem(totemId, listOf(courser, lions), mapOf(courser to 3, lions to 0)).error shouldNotBe null
        d.activateTotem(totemId, listOf(courser, lions), mapOf(courser to 3)).error shouldNotBe null
        d.stackSize shouldBe 0
    }

    test("a target removed in response to an activation loses its share; nothing is re-divided") {
        val d = newGame()
        val totemId = d.putPermanentOnBattlefield(d.player1, "Counter Totem Probe")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.activateTotem(totemId, listOf(courser, lions), mapOf(courser to 1, lions to 2)).error shouldBe null

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(lions)).error shouldBe null
        d.bothPass().error shouldBe null // Bolt resolves, Lions die
        d.bothPass().error shouldBe null // the ability resolves

        (lions in d.state.getBattlefield()) shouldBe false
        d.pendingDecision shouldBe null
        d.plusOnes(courser) shouldBe 1
    }

    test("an activation that leaves the division out still divides at resolution") {
        val d = newGame()
        val totemId = d.putPermanentOnBattlefield(d.player1, "Counter Totem Probe")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.activateTotem(totemId, listOf(courser, lions), null).error shouldBe null
        d.bothPass().error shouldBe null

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.context.phase shouldBe DecisionPhase.RESOLUTION
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 1, lions to 2))).error shouldBe null
        d.plusOnes(courser) shouldBe 1
        d.plusOnes(lions) shouldBe 2
    }
})
