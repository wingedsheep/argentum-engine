package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`40k`.cards.GhyrsonStarnKelermorph
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ghyrson Starn, Kelermorph — "Whenever another source you control deals exactly 1 damage to a
 * permanent or player, Ghyrson Starn deals 2 damage to that permanent or player."
 *
 * Ruling: a source dealing exactly 1 damage to several permanents and/or players at once triggers
 * it that many times — including combat damage one attacker splits between a blocker and the
 * defending player, which an unrestricted "deals damage" trigger would fold into one.
 */
class GhyrsonStarnKelermorphScenarioTest : FunSpec({

    val ping = card("Test Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target(Targets.Any)
            effect = Effects.DealDamage(1, victim)
        }
    }

    val sweep = card("Test Opponent Sweep") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.opponentControls()))
        }
    }

    val trampler = card("Test Small Trampler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        keywords(Keyword.TRAMPLE)
    }

    val wall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    val chump = card("Test Chump") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 1
    }

    val weaken = card("Test Weaken") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val victim = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.ModifyStats(-2, 0, victim)
        }
    }

    val bruiser = card("Test Bruiser Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 1
        toughness = 3
    }

    val pinger = card("Test Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }

    fun duel(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(GhyrsonStarnKelermorph, ping, sweep, trampler, wall, chump, weaken, bruiser, pinger))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 30) passPriority(state.priorityPlayerId!!)
        state.stack.isEmpty() shouldBe true
    }

    fun GameTestDriver.getDamage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.cast(caster: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val spell = putCardInHand(caster, name)
        giveMana(caster, Color.RED)
        castSpell(caster, spell, targets).outcome shouldBe Outcome.Done
        resolveStack()
    }

    fun GameTestDriver.attackInto(attacker: EntityId, blocker: EntityId?) {
        removeSummoningSickness(attacker)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).outcome shouldBe Outcome.Done
        bothPass()
        val blocks = if (blocker == null) emptyMap() else mapOf(blocker to listOf(attacker))
        declareBlockers(player2, blocks).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.END_COMBAT)
    }

    test("a spell you control dealing exactly 1 damage to a player adds 2 more to that player") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        d.cast(d.player1, "Test Ping", listOf(d.player2))
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("1 damage to a permanent adds 2 more to that permanent") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        val wall = d.putCreatureOnBattlefield(d.player2, "Test Wall")
        d.cast(d.player1, "Test Ping", listOf(wall))
        d.getDamage(wall) shouldBe 3
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("damage other than exactly 1 does not trigger") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        d.cast(d.player1, "Lightning Bolt", listOf(d.player2))
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("an opponent's source dealing 1 damage does not trigger") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        d.passPriority(d.player1)
        d.cast(d.player2, "Test Ping", listOf(d.player1))
        d.getLifeTotal(d.player1) shouldBe 19
    }

    test("1 damage to each of several permanents triggers once per permanent") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        val w1 = d.putCreatureOnBattlefield(d.player2, "Test Wall")
        val w2 = d.putCreatureOnBattlefield(d.player2, "Test Wall")
        d.cast(d.player1, "Test Opponent Sweep")
        d.getDamage(w1) shouldBe 3
        d.getDamage(w2) shouldBe 3
    }

    test("combat damage split 1 to a blocker and 1 to the player triggers twice, not once for 2") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        val attacker = d.putCreatureOnBattlefield(d.player1, "Test Small Trampler")
        val blocker = d.putCreatureOnBattlefield(d.player2, "Test Chump")
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.bothPass()
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.END_COMBAT)
        // 1 tramples over, then Ghyrson's trigger for the player adds 2.
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("Ghyrson dealing exactly 1 damage itself is not another source") {
        val d = duel()
        val ghyrson = d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        d.cast(d.player1, "Test Weaken", listOf(ghyrson))
        d.attackInto(ghyrson, blocker = null)
        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("a token that dies to the same combat damage it dealt still triggers") {
        val d = duel()
        d.putCreatureOnBattlefield(d.player1, "Ghyrson Starn, Kelermorph")
        val token = d.putCreatureOnBattlefield(d.player1, "Test Pinger")
        d.replaceState(d.state.updateEntity(token) { it.with(TokenComponent) })
        val bruiser = d.putCreatureOnBattlefield(d.player2, "Test Bruiser Wall")
        d.attackInto(token, blocker = bruiser)
        // The token's 1 damage and Ghyrson's 2 kill the 1/3; the token died to the 1/3's 1 damage.
        d.findPermanent(d.player1, "Test Pinger") shouldBe null
        d.findPermanent(d.player2, "Test Bruiser Wall") shouldBe null
    }
})
