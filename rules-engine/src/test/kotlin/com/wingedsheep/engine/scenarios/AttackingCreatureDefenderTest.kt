package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Defending player" and "the player or planeswalker it's attacking" in a per-attacker
 * "whenever a creature you control attacks" trigger (CR 506.2, 508.5, 802.2a).
 *
 * Pins that both name the *triggering attacker's* defender — not the source's, even when the
 * source is attacking someone else (Brutal Hordechief, Silumgar, the Drifting Death) — and that
 * [EffectTarget.AttackedBy] names an attacked planeswalker itself, keeps the defender after the
 * attacker has left the battlefield, and names nothing for a creature that isn't attacking.
 */
class AttackingCreatureDefenderTest : FunSpec({

    /** "Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life." */
    val drainer = card("Test Defender Drainer") {
        manaCost = "{3}"
        typeLine = "Creature — Goblin"
        power = 3
        toughness = 3
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            effect = Effects.Composite(listOf(
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.DefendingPlayer)),
                Effects.GainLife(1),
            ))
        }
    }

    /** "Whenever a creature you control attacks, creatures defending player controls get -1/-1 until end of turn." */
    val shrinker = card("Test Defender Shrinker") {
        manaCost = "{3}"
        typeLine = "Creature — Dragon"
        power = 3
        toughness = 7
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            effect = Patterns.Group.modifyStatsForAll(
                -1, -1,
                GroupFilter(GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))),
            )
        }
    }

    /** "Whenever a creature you control attacks, this deals 1 damage to the player or planeswalker that creature is attacking." */
    val bomber = card("Test Attacked Bomber") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            effect = Effects.DealDamage(1, EffectTarget.AttackedBy())
        }
    }

    /** Sacrifices the attacker first, then deals the damage — the defender must survive the departure. */
    val sacBomber = card("Test Sacrificing Bomber") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
            effect = Effects.Composite(listOf(
                Effects.SacrificeTarget(EffectTarget.TriggeringEntity),
                Effects.DealDamage(1, EffectTarget.AttackedBy()),
            ))
        }
    }

    /** "{T}: this deals 1 damage to the player or planeswalker this creature is attacking." */
    val pingerCard = card("Test Self Attacked Pinger") {
        manaCost = "{2}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            effect = Effects.DealDamage(1, EffectTarget.AttackedBy(EffectTarget.Self))
        }
    }

    val walker = card("Test Defending Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val cards = TestCards.all + drainer + shrinker + bomber + sacBomber + pingerCard + walker

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun threePlayer(): Triple<GameTestDriver, EntityId, Pair<EntityId, EntityId>> {
        val d = GameTestDriver()
        d.registerCards(cards)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1
        return Triple(d, p1, p2 to p3)
    }

    test("defending player is the triggering attacker's, even when the source attacks someone else") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        val source = d.ready(p1, "Test Defender Drainer")
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(source to p2, bears to p3)).error shouldBe null
        d.stackSize shouldBe 2
        d.resolveAll()
        withClue("CR 508.5: the source's own trigger drains the player it attacks") { d.getLifeTotal(p2) shouldBe 19 }
        withClue("CR 508.5: the Bears' trigger drains the player the Bears attack, not the source's") {
            d.getLifeTotal(p3) shouldBe 19
        }
        d.getLifeTotal(p1) shouldBe 22
    }

    test("a non-attacking source reads each attacker's own defender") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putCreatureOnBattlefield(p1, "Test Defender Drainer")
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")
        val c = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p2, b to p3, c to p3)).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 19
        d.getLifeTotal(p3) shouldBe 18
    }

    test("creatures defending player controls: each trigger shrinks its own attacker's defender") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        val source = d.ready(p1, "Test Defender Shrinker")
        val bears = d.ready(p1, "Grizzly Bears")
        // 3/7s so two -1/-1s leave them on the battlefield to be read; their own triggers only
        // watch their controllers' attacks.
        val p2Creature = d.putCreatureOnBattlefield(p2, "Test Defender Shrinker")
        val p3Creature = d.putCreatureOnBattlefield(p3, "Test Defender Shrinker")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(source to p2, bears to p3)).error shouldBe null
        d.resolveAll()
        withClue("the source's own trigger shrinks only p2") {
            d.state.projectedState.getPower(p2Creature) shouldBe 2
        }
        withClue("CR 508.5: the Bears' trigger shrinks the Bears' defender, not the source's") {
            d.state.projectedState.getPower(p3Creature) shouldBe 2
        }
        d.state.projectedState.getPower(bears) shouldBe 2
    }

    test("the player or planeswalker it's attacking: each attacker's own player") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Attacked Bomber")
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p2, b to p3)).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 19
        d.getLifeTotal(p3) shouldBe 19
    }

    test("an attacked planeswalker takes the damage, not its controller") {
        val (d, p1, others) = threePlayer()
        val (p2, _) = others
        d.putPermanentOnBattlefield(p1, "Test Attacked Bomber")
        val pw = d.putPermanentOnBattlefield(p2, "Test Defending Walker")
        d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to pw)).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 20
        d.state.getEntity(pw)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 2
    }

    test("the attacker leaving the battlefield keeps its defender (last-known information)") {
        val (d, p1, others) = threePlayer()
        val (_, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Sacrificing Bomber")
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        d.resolveAll()
        d.getGraveyardCardNames(p1).contains("Grizzly Bears") shouldBe true
        d.getLifeTotal(p3) shouldBe 19
    }

    test("a creature that isn't attacking attacks nothing") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        val pinger = d.ready(p1, "Test Self Attacked Pinger")
        d.submit(ActivateAbility(p1, pinger, pingerCard.script.activatedAbilities.single().id)).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 20
        d.getLifeTotal(p3) shouldBe 20
        d.getLifeTotal(p1) shouldBe 20
    }
})
