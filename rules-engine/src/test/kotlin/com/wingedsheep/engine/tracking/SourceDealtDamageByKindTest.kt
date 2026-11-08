package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DealtDamageToThisGameComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `SourceDealtDamageToPlayerThisTurn(player, damageType)` — the source's per-recipient damage
 * memory split by kind: `Combat` answers only for combat damage (CR 510.2), `NonCombat` only for
 * the rest, and `Any` (the default, Wicked Akuba) for either. Each kind is stamped with its own
 * turn, so last turn's combat damage never answers a combat question this turn.
 */
class SourceDealtDamageByKindTest : FunSpec({

    // 2/2. Ability 0: ping target player for 1. Abilities 1–3: "target player dealt <kind> damage by
    // this creature this turn loses 1 life", for Combat, NonCombat and Any. Ability 4 below.
    val probe = card("Test Damage Kind Probe") {
        manaCost = "{1}"
        typeLine = "Creature — Shaman"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            val victim = target(Targets.Player)
            effect = Effects.DealDamage(1, victim)
        }
        for (kind in listOf(DamageType.Combat, DamageType.NonCombat, DamageType.Any)) {
            activatedAbility {
                cost = Costs.Free
                val victim = target(
                    TargetPlayer(
                        restriction = Conditions.candidateWasDealtDamageBySourceThisTurn(kind),
                        descriptionOverride = "target player dealt ${kind.description} damage by this creature this turn",
                    )
                )
                effect = Effects.LoseLife(1, victim)
            }
        }
        // Ability 4: Hope of Ghirapur's shape — sacrifice it as the cost, combat-only target.
        activatedAbility {
            cost = Costs.SacrificeSelf
            val victim = target(
                TargetPlayer(
                    restriction = Conditions.candidateWasDealtDamageBySourceThisTurn(DamageType.Combat),
                    descriptionOverride = "target player dealt combat damage by this creature this turn",
                )
            )
            effect = Effects.LoseLife(1, victim)
        }
    }
    val ping = probe.activatedAbilities[0].id
    val combatDrain = probe.activatedAbilities[1].id
    val noncombatDrain = probe.activatedAbilities[2].id
    val anyDrain = probe.activatedAbilities[3].id
    val sacrificeCombatDrain = probe.activatedAbilities[4].id

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(probe))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activate(controller: EntityId, source: EntityId, ability: AbilityId, victim: EntityId) =
        submit(ActivateAbility(controller, source, ability, targets = listOf(ChosenTarget.Player(victim))))

    fun GameTestDriver.resolve() {
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.attack(me: EntityId, them: EntityId, creature: EntityId) {
        removeSummoningSickness(creature)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(creature), them).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    test("combat damage answers Combat and Any, not NonCombat") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, probe.name)

        d.attack(me, them, creature)
        d.getLifeTotal(them) shouldBe 18
        d.activate(me, creature, noncombatDrain, them).error shouldNotBe null
        d.activate(me, creature, combatDrain, them).error shouldBe null
        d.resolve()
        d.activate(me, creature, anyDrain, them).error shouldBe null
        d.resolve()
        d.getLifeTotal(them) shouldBe 16
    }

    test("noncombat damage answers NonCombat and Any, not Combat") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, probe.name)

        d.activate(me, creature, ping, them).error shouldBe null
        d.resolve()
        d.activate(me, creature, combatDrain, them).error shouldNotBe null
        d.activate(me, creature, noncombatDrain, them).error shouldBe null
        d.resolve()
        d.activate(me, creature, anyDrain, them).error shouldBe null
        d.resolve()
        d.getLifeTotal(them) shouldBe 17
    }

    test("each kind keeps its own turn stamp") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, probe.name)

        d.attack(me, them, creature)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, them)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.activate(me, creature, ping, them).error shouldBe null
        d.resolve()

        withClue("last turn's combat damage is stale; this turn's ping is noncombat") {
            d.activate(me, creature, combatDrain, them).error shouldNotBe null
        }
        val memory = d.state.getEntity(creature)!!.get<DealtDamageToThisGameComponent>()!!
        memory.dealtDamageToOnTurn(them, d.state.turnNumber, DamageType.Any) shouldBe true
        memory.dealtDamageToOnTurn(them, d.state.turnNumber, DamageType.NonCombat) shouldBe true
        memory.dealtDamageToOnTurn(them, d.state.turnNumber, DamageType.Combat) shouldBe false
    }

    test("the combat record survives a sacrifice cost moving the source to the graveyard") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, probe.name)

        d.attack(me, them, creature)
        d.activate(me, creature, sacrificeCombatDrain, them).error shouldBe null
        withClue("sacrificed as the cost") { d.state.getBattlefield().contains(creature) shouldBe false }
        d.resolve()
        withClue("the target is still legal on resolution (last-known information)") {
            d.getLifeTotal(them) shouldBe 17
        }
    }
})
