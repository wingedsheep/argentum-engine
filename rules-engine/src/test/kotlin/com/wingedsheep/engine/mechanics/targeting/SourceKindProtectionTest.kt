package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection and hexproof from a *kind of source* (CR 702.16a, 702.11d) — spells, permanents that
 * were cast this turn, activated abilities, triggered abilities. Each leg of protection is pinned:
 * targeting (702.16b), damage (702.16e), blocking (702.16f), plus hexproof's opponents-only reach.
 */
class SourceKindProtectionTest : FunSpec({

    // Emrakul, the World Anew's protection on a 2/2 body, so 2 damage would kill it.
    val titan = card("Test Warded Titan") {
        manaCost = "{2}"
        typeLine = "Creature — Eldrazi"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.Spells))
        keywordAbility(KeywordAbility.Protection(ProtectionScope.PermanentsCastThisTurn))
    }
    // Volatile Stormdrake's hexproof on a 2/2 body.
    val drake = card("Test Warded Drake") {
        manaCost = "{2}"
        typeLine = "Creature — Drake"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Hexproof(ProtectionScope.ActivatedAbilities))
        keywordAbility(KeywordAbility.Hexproof(ProtectionScope.TriggeredAbilities))
    }
    val pinger = card("Test Pinger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val shocker = card("Test Shock Elemental") {
        manaCost = "{1}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            val t = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.DealDamage(2, t)
        }
    }
    val sweeper = card("Test Pyroclasm") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(2, EffectTarget.IterationEntity)
            )
        }
    }
    val raider = card("Test Hasty Raider") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 3
        toughness = 3
        keywords(Keyword.HASTE)
    }
    val flashBear = card("Test Flash Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.FLASH)
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(titan, drake, pinger, shocker, sweeper, raider, flashBear))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val pingId = pinger.script.activatedAbilities.single().id
    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.ping(player: EntityId, source: EntityId, target: EntityId) = run {
        giveColorlessMana(player, 1)
        submit(ActivateAbility(player, source, pingId, listOf(ChosenTarget.Permanent(target))))
    }
    fun GameTestDriver.offeredTargets(player: EntityId, matches: (com.wingedsheep.engine.core.GameAction) -> Boolean) =
        legalActions(player).first { matches(it.action) }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }

    context("protection from spells") {
        test("no spell can target it — neither its controller's nor an opponent's — and it isn't offered") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player1, titan.name)
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            val offered = d.offeredTargets(d.player1) { it is CastSpell && it.cardId == bolt }
            offered.contains(warded) shouldBe false
            offered.contains(bear) shouldBe true
            d.castSpell(d.player1, bolt, listOf(warded)).error shouldNotBe null
        }

        test("damage from a spell that doesn't target is prevented") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player1, titan.name)
            d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val spell = d.putCardInHand(d.player1, sweeper.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, spell).error shouldBe null
            d.bothPass()
            d.findPermanent(d.player1, titan.name) shouldBe warded
            d.damage(warded) shouldBe 0
            d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        }

        test("an ability of a permanent that wasn't cast this turn can target and damage it") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player1, titan.name)
            val source = d.putPermanentOnBattlefield(d.player1, pinger.name)
            d.ping(d.player1, source, warded).error shouldBe null
            d.bothPass()
            d.damage(warded) shouldBe 1
        }
    }

    context("protection from permanents that were cast this turn") {
        test("an ability of a permanent cast this turn can't target it, but can target others") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player1, titan.name)
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val card = d.putCardInHand(d.player1, pinger.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, card).error shouldBe null
            d.bothPass()
            val source = d.findPermanent(d.player1, pinger.name)!!
            d.giveColorlessMana(d.player1, 1)
            val offered = d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == source }
            offered.contains(warded) shouldBe false
            offered.contains(bear) shouldBe true
            d.ping(d.player1, source, warded).error shouldNotBe null
            d.ping(d.player1, source, bear).error shouldBe null
        }

        test("a creature cast this turn can't block it; one put onto the battlefield this turn can") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player1, titan.name)
            d.removeSummoningSickness(warded)
            val putIn = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            d.passPriority(d.player1)
            val flash = d.putCardInHand(d.player2, flashBear.name)
            d.giveColorlessMana(d.player2, 1)
            d.castSpell(d.player2, flash).error shouldBe null
            d.bothPass()
            val castBear = d.findPermanent(d.player2, flashBear.name)!!
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(warded), d.player2).error shouldBe null
            d.passPriorityUntil(Step.DECLARE_BLOCKERS)
            withClue("the flash creature was cast this turn") {
                d.declareBlockers(d.player2, mapOf(castBear to listOf(warded))).error shouldNotBe null
            }
            d.declareBlockers(d.player2, mapOf(putIn to listOf(warded))).error shouldBe null
        }

        test("combat damage from a creature cast this turn is prevented") {
            val d = driver()
            val warded = d.putCreatureOnBattlefield(d.player2, titan.name)
            val card = d.putCardInHand(d.player1, raider.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, card).error shouldBe null
            d.bothPass()
            val attacker = d.findPermanent(d.player1, raider.name)!!
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
            d.passPriorityUntil(Step.DECLARE_BLOCKERS)
            d.declareBlockers(d.player2, mapOf(warded to listOf(attacker))).error shouldBe null
            d.passPriorityUntil(Step.END_COMBAT)
            d.findPermanent(d.player2, titan.name) shouldBe warded
            d.damage(attacker) shouldBe 2
        }
    }

    context("hexproof from activated and triggered abilities") {
        test("an opponent's activated ability can't target it; its controller's can") {
            val d = driver()
            val theirs = d.putCreatureOnBattlefield(d.player2, drake.name)
            val mine = d.putCreatureOnBattlefield(d.player1, drake.name)
            val source = d.putPermanentOnBattlefield(d.player1, pinger.name)
            d.giveColorlessMana(d.player1, 1)
            val offered = d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == source }
            offered.contains(theirs) shouldBe false
            offered.contains(mine) shouldBe true
            d.ping(d.player1, source, theirs).error shouldNotBe null
            d.ping(d.player1, source, mine).error shouldBe null
        }

        test("an opponent's triggered ability can't target it") {
            val d = driver()
            val theirs = d.putCreatureOnBattlefield(d.player2, drake.name)
            val card = d.putCardInHand(d.player1, shocker.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, card).error shouldBe null
            d.bothPass()
            d.state.stack.size shouldBe 0
            d.state.pendingDecision shouldBe null
            d.damage(theirs) shouldBe 0
        }

        test("an opponent's spell can still target it") {
            val d = driver()
            val theirs = d.putCreatureOnBattlefield(d.player2, drake.name)
            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.castSpell(d.player1, bolt, listOf(theirs)).error shouldBe null
            d.bothPass()
            d.findPermanent(d.player2, drake.name) shouldBe null
        }
    }

    test("a target that gains the protection before resolution is dropped (CR 608.2b)") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, pinger.name)
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.ping(d.player1, source, theirs).error shouldBe null
        // Grant the quality mid-stack the way the projector reads it: a printed-protection marker.
        d.addComponent(
            theirs,
            com.wingedsheep.engine.state.components.identity.HexproofFromComponent(
                sourceKinds = setOf(SourceKind.ACTIVATED_ABILITY)
            )
        )
        d.bothPass()
        d.damage(theirs) shouldBe 0
    }
})
