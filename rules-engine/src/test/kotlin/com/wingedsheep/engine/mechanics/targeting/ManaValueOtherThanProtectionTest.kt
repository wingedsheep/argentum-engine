package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofFromToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection from each mana value other than N — `ProtectionScope.ManaValueOtherThan` (Haktos the
 * Unscarred). Mana value is a characteristic (CR 202.3), so every leg applies: targeting by spells
 * of another mana value and by abilities from sources of one (CR 702.16b), damage from such
 * sources (CR 702.16e). A spell's {X} counts as announced (CR 202.3e); a source with no mana value
 * matches nothing. The blocking leg and the random choice are pinned in HaktosTheUnscarredScenarioTest.
 */
class ManaValueOtherThanProtectionTest : FunSpec({

    val warded = card("Test Two-Only Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantProtectionFromToGroup(ProtectionScope.ManaValueOtherThan(2), GroupFilter.source())
        }
    }
    fun pinger(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Artifact"
        activatedAbility {
            this.cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val twoPinger = pinger("Test Two Pinger", "{2}")
    val zeroPinger = pinger("Test Zero Pinger", "{0}")
    val pingers = listOf(twoPinger, zeroPinger)
    fun sweeper(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(1, EffectTarget.IterationEntity)
            )
        }
    }
    val twoSweeper = sweeper("Test Two Sweeper", "{2}")
    val threeSweeper = sweeper("Test Three Sweeper", "{3}")
    val twoBolt = card("Test Two Bolt") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val xBlast = card("Test X Blast") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(warded, twoPinger, zeroPinger, twoSweeper, threeSweeper, twoBolt, xBlast))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.ping(player: EntityId, source: EntityId, target: EntityId) = run {
        giveColorlessMana(player, 1)
        val name = state.getEntity(source)!!.get<CardComponent>()!!.name
        val abilityId = pingers.first { it.name == name }.script.activatedAbilities.single().id
        submit(ActivateAbility(player, source, abilityId, listOf(ChosenTarget.Permanent(target))))
    }

    test("a spell of another mana value can't target it and doesn't offer it; one of the number can") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, warded.name)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 3)
        val offered = d.legalActions(d.player1).first { (it.action as? CastSpell)?.cardId == bolt }
            .let { la -> la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty() }
        offered.contains(bear) shouldBe false
        d.castSpell(d.player1, bolt, listOf(bear)).error shouldNotBe null
        val two = d.putCardInHand(d.player1, twoBolt.name)
        d.castSpell(d.player1, two, listOf(bear)).error shouldBe null
        d.bothPass()
        d.damage(bear) shouldBe 1
    }

    test("an ability from a source of another mana value can't target it; one from a source of the number can") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, warded.name)
        val zero = d.putPermanentOnBattlefield(d.player1, zeroPinger.name)
        val two = d.putPermanentOnBattlefield(d.player1, twoPinger.name)
        d.ping(d.player1, zero, bear).error shouldNotBe null
        d.ping(d.player1, two, bear).error shouldBe null
        d.bothPass()
        d.damage(bear) shouldBe 1
    }

    test("damage from an untargeted spell of another mana value is prevented; of the number it isn't") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, warded.name)
        val three = d.putCardInHand(d.player1, threeSweeper.name)
        d.giveColorlessMana(d.player1, 3)
        d.castSpell(d.player1, three).error shouldBe null
        d.bothPass()
        d.damage(bear) shouldBe 0
        val two = d.putCardInHand(d.player1, twoSweeper.name)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, two).error shouldBe null
        d.bothPass()
        d.damage(bear) shouldBe 1
    }

    test("an X spell is offered before X is announced; the announced X then decides") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, warded.name)
        val blast = d.putCardInHand(d.player1, xBlast.name)
        d.giveMana(d.player1, Color.RED, 3)
        val offered = d.legalActions(d.player1).first { (it.action as? CastSpell)?.cardId == blast }
            .let { la -> la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty() }
        offered.contains(bear) shouldBe true
        // X=0: mana value 1 — protected. X=1: mana value 2 — the number, so legal.
        d.castXSpell(d.player1, blast, 0, listOf(bear)).error shouldNotBe null
        d.castXSpell(d.player1, blast, 1, listOf(bear)).error shouldBe null
        d.bothPass()
        d.damage(bear) shouldBe 1
    }

    test("a source with no mana value matches nothing") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, warded.name)
        SourceKindProtection.isProtectedFromObject(d.state, bear, d.player1) shouldBe false
    }

    test("it is a granted static only: no printed keyword and no hexproof spelling") {
        shouldThrow<IllegalArgumentException> { KeywordAbility.Protection(ProtectionScope.ManaValueOtherThan(2)) }
        shouldThrow<IllegalArgumentException> { KeywordAbility.Hexproof(ProtectionScope.ManaValueOtherThan(2)) }
        shouldThrow<IllegalArgumentException> { GrantHexproofFromToGroup(ProtectionScope.ManaValueOtherThan(2)) }
    }
})
