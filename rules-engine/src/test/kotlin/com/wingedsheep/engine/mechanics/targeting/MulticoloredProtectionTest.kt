package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection from multicolored (CR 702.16a; a multicolored object has two or more colors, CR 105.1)
 * — Argentum Masticore. Pins every leg of DEBT against a two-colour source, and that a monocolored
 * or colorless source is unaffected.
 */
class MulticoloredProtectionTest : FunSpec({

    val warded = card("Test Multicolor-Warded Golem") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))
    }
    val gildedBolt = card("Test Gilded Bolt") {
        manaCost = "{R}{W}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }
    val gildedSweep = card("Test Gilded Sweep") {
        manaCost = "{R}{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(2, EffectTarget.IterationEntity)
            )
        }
    }
    val colorlessBolt = card("Test Colorless Bolt") {
        manaCost = "{2}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }
    val gildedBear = card("Test Gilded Bear") {
        manaCost = "{G}{W}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(warded, gildedBolt, gildedSweep, colorlessBolt, gildedBear))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.offeredTargets(player: EntityId, cardId: EntityId) =
        legalActions(player).first { (it.action as? CastSpell)?.cardId == cardId }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }

    test("a multicolored spell can't target it and doesn't offer it") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player2, warded.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, gildedBolt.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.giveMana(d.player1, Color.WHITE, 1)
        val offered = d.offeredTargets(d.player1, spell)
        offered.contains(golem) shouldBe false
        offered.contains(bear) shouldBe true
        d.castSpell(d.player1, spell, listOf(golem)).error shouldNotBe null
    }

    test("monocolored and colorless spells can target and damage it") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player2, warded.name)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(golem)).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player2, warded.name) shouldBe null

        val golem2 = d.putCreatureOnBattlefield(d.player2, warded.name)
        val clear = d.putCardInHand(d.player1, colorlessBolt.name)
        d.giveColorlessMana(d.player1, 2)
        d.castSpell(d.player1, clear, listOf(golem2)).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player2, warded.name) shouldBe null
    }

    test("damage from a multicolored spell that doesn't target is prevented") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player2, warded.name)
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, gildedSweep.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player2, warded.name) shouldBe golem
        d.damage(golem) shouldBe 0
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
    }

    test("a multicolored creature can't block it; a monocolored one can") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player1, warded.name)
        d.removeSummoningSickness(golem)
        val gilded = d.putCreatureOnBattlefield(d.player2, gildedBear.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(golem), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("the green-white blocker is multicolored") {
            d.declareBlockers(d.player2, mapOf(gilded to listOf(golem))).error shouldNotBe null
        }
        d.declareBlockers(d.player2, mapOf(bear to listOf(golem))).error shouldBe null
    }

    test("combat damage from a multicolored creature is prevented") {
        val d = driver()
        val golem = d.putCreatureOnBattlefield(d.player2, warded.name)
        val attacker = d.putCreatureOnBattlefield(d.player1, gildedBear.name)
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(golem to listOf(attacker))).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)
        d.findPermanent(d.player2, warded.name) shouldBe golem
        d.damage(golem) shouldBe 0
        d.damage(attacker) shouldBe 2
    }
})
