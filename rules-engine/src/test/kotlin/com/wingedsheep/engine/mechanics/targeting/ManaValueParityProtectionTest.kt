package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
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
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.Parity
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection and hexproof from each mana value of a parity — `ProtectionScope.ManaValueParity`
 * (Lavabrink Venturer). Mana value is a characteristic (CR 202.3), so every leg applies:
 * targeting by spells of that parity and by abilities from sources of it (CR 702.16b), damage
 * from such sources (CR 702.16e), and hexproof's opponents-only reach (CR 702.11d). The X-spell
 * and blocking legs are pinned in LavabrinkVenturerScenarioTest.
 */
class ManaValueParityProtectionTest : FunSpec({

    val evenWarded = card("Test Even-Warded Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.ManaValueParity(Parity.EVEN)))
    }
    val oddHexproof = card("Test Odd-Hexproof Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Hexproof(ProtectionScope.ManaValueParity(Parity.ODD)))
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
    val evenPinger = pinger("Test Even Pinger", "{2}")
    val oddPinger = pinger("Test Odd Pinger", "{1}")
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
    val evenBolt = card("Test Even Bolt") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val evenSweeper = sweeper("Test Even Sweeper", "{2}")
    val oddSweeper = sweeper("Test Odd Sweeper", "{1}")

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(evenWarded, oddHexproof, evenBolt, evenPinger, oddPinger, evenSweeper, oddSweeper))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.ping(player: EntityId, source: EntityId, target: EntityId) = run {
        giveColorlessMana(player, 1)
        val abilityId = state.getEntity(source)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.let { c ->
            (if (c.name == evenPinger.name) evenPinger else oddPinger).script.activatedAbilities.single().id
        }
        submit(ActivateAbility(player, source, abilityId, listOf(ChosenTarget.Permanent(target))))
    }

    context("protection from each even mana value") {
        test("a spell of even mana value can't target it and doesn't offer it; an odd one can") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player2, evenWarded.name)
            val shock = d.putCardInHand(d.player1, evenBolt.name)
            d.giveMana(d.player1, Color.RED, 2)
            val offered = d.legalActions(d.player1).first { (it.action as? CastSpell)?.cardId == shock }
                .let { la -> la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty() }
            offered.contains(warded) shouldBe false
            d.castSpell(d.player1, shock, listOf(warded)).error shouldNotBe null
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.castSpell(d.player1, bolt, listOf(warded)).error shouldBe null
        }

        test("an ability from a source of even mana value can't target it; one from an odd source can") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player2, evenWarded.name)
            val even = d.putPermanentOnBattlefield(d.player1, evenPinger.name)
            val odd = d.putPermanentOnBattlefield(d.player1, oddPinger.name)
            d.ping(d.player1, even, warded).error shouldNotBe null
            d.ping(d.player1, odd, warded).error shouldBe null
            d.bothPass()
            d.damage(warded) shouldBe 1
        }

        test("damage from an even spell that doesn't target is prevented; from an odd one it isn't") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player2, evenWarded.name)
            val even = d.putCardInHand(d.player1, evenSweeper.name)
            d.giveColorlessMana(d.player1, 2)
            d.castSpell(d.player1, even).error shouldBe null
            d.bothPass()
            d.damage(warded) shouldBe 0
            val odd = d.putCardInHand(d.player1, oddSweeper.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, odd).error shouldBe null
            d.bothPass()
            d.damage(warded) shouldBe 1
        }

        test("a source with no mana value at all matches neither parity") {
            val d = driver()
            SourceKindProtection.manaValue(d.state, d.player1) shouldBe null
        }
    }

    context("hexproof from each odd mana value") {
        test("an opponent's odd spell can't target it; its controller's can") {
            val d = driver()
            val theirs = d.putCreatureOnBattlefield(d.player2, oddHexproof.name)
            val mine = d.putCreatureOnBattlefield(d.player1, oddHexproof.name)
            d.giveMana(d.player1, Color.RED, 2)
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.castSpell(d.player1, bolt, listOf(theirs)).error shouldNotBe null
            d.castSpell(d.player1, bolt, listOf(mine)).error shouldBe null
        }
    }
})
