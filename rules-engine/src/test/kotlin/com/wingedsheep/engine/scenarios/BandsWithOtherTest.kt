package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Bands with other [quality]" (CR 702.22b) — the special form of banding.
 *
 * - CR 702.22c: one or more attacking [quality] creatures with "bands with other [quality]" and
 *   any number of other attacking [quality] creatures may attack as a band.
 * - CR 702.22b: losing banding loses every "bands with other" ability too.
 * - CR 702.22j: an attacker blocked by both a [quality] creature with "bands with other
 *   [quality]" and another [quality] creature has its damage divided by the defending player.
 * - CR 702.22k: a blocker blocking such a pair has its damage divided by the active player.
 */
class BandsWithOtherTest : FunSpec({

    val legendaryBander = card("Test Legendary Bander") {
        manaCost = "{1}{W}"
        typeLine = "Legendary Creature — Human Knight"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bandsWithOther(Supertype.LEGENDARY))
    }
    val legendA = card("Test Legend A") {
        manaCost = "{1}{W}"
        typeLine = "Legendary Creature — Human Soldier"
        power = 2
        toughness = 2
    }
    val legendB = card("Test Legend B") {
        manaCost = "{1}{W}"
        typeLine = "Legendary Creature — Human Cleric"
        power = 2
        toughness = 2
    }
    val grunt = card("Test Grunt") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }
    val fogey = card("Test Fogey") {
        manaCost = "{G}"
        typeLine = "Creature — Dinosaur"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.bandsWithOther(Subtype("Dinosaur")))
    }
    val dinosaur = card("Test Dinosaur") {
        manaCost = "{G}"
        typeLine = "Creature — Dinosaur"
        power = 1
        toughness = 1
    }
    val giant = card("Test Giant") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Giant"
        power = 6
        toughness = 6
    }
    val bandingBan = card("Test Banding Ban") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        staticAbility { ability = RemoveKeywordStatic(Keyword.BANDING, GroupFilter(GameObjectFilter.Creature)) }
    }
    val familyBan = card("Test Family Ban") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        staticAbility {
            ability = RemoveKeywordStatic(KeywordAbility.BandsWithOther.FAMILY_KEYWORD, GroupFilter(GameObjectFilter.Creature))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(legendaryBander, legendA, legendB, grunt, fogey, dinosaur, giant, bandingBan, familyBan)
            .forEach(driver::registerCard)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        return driver
    }

    fun GameTestDriver.creatures(player: EntityId, vararg names: String): List<EntityId> =
        names.map { name -> putCreatureOnBattlefield(player, name).also(::removeSummoningSickness) }

    fun GameTestDriver.combatResolution(): CombatResolutionDecision {
        var passes = 0
        while (passes++ < 50) {
            when (val decision: PendingDecision? = state.pendingDecision) {
                null -> submit(PassPriority(state.priorityPlayerId!!))
                is OrderObjectsDecision ->
                    submitDecision(decision.playerId, OrderedResponse(decision.id, decision.objects))
                else -> return decision.shouldBeInstanceOf<CombatResolutionDecision>()
            }
        }
        error("No combat resolution decision was emitted")
    }

    test("CR 702.22c: legendary creatures without banding band when one has bands with other legendary creatures") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Legendary Bander", "Test Legend A", "Test Legend B")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, opponent).outcome shouldBe Outcome.Done

        val bandIds = band.map { driver.state.getEntity(it)?.get<AttackingComponent>()?.bandId }
        bandIds.first().shouldNotBeNull()
        bandIds.toSet().size shouldBe 1
    }

    test("CR 702.22c: a creature without the quality can't join a bands-with-other band") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Legendary Bander", "Test Legend A", "Test Grunt")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, opponent).error shouldNotBe null
    }

    test("CR 702.22c: sharing the quality is not enough without a member that bands with other") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Legend A", "Test Legend B")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, opponent).error shouldNotBe null
    }

    test("a creature-type quality: bands with other Dinosaurs") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Fogey", "Test Dinosaur", "Test Dinosaur")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, opponent).outcome shouldBe Outcome.Done
    }

    test("CR 702.22b: losing banding loses bands with other as well") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Legendary Bander", "Test Legend A")
        val keyword = KeywordAbility.bandsWithOther(Supertype.LEGENDARY).projectedKeyword
        driver.state.projectedState.hasKeyword(band.first(), keyword) shouldBe true

        driver.putPermanentOnBattlefield(opponent, "Test Banding Ban")
        driver.state.projectedState.hasKeyword(band.first(), keyword) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, opponent).error shouldNotBe null
    }

    test("losing all \"bands with other\" abilities strips every quality") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (bander, fogeyId) = driver.creatures(active, "Test Legendary Bander", "Test Fogey")

        driver.putPermanentOnBattlefield(opponent, "Test Family Ban")
        val projected = driver.state.projectedState
        projected.getKeywords(bander).filter { KeywordAbility.BandsWithOther.isBandsWithOther(it) } shouldBe emptyList()
        projected.getKeywords(fogeyId).filter { KeywordAbility.BandsWithOther.isBandsWithOther(it) } shouldBe emptyList()
    }

    test("CR 702.22j: the defending player divides an attacker's damage blocked by a bands-with-other pair") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (giantId) = driver.creatures(active, "Test Giant")
        val (bander, legend) = driver.creatures(defender, "Test Legendary Bander", "Test Legend A")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(giantId), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(bander to listOf(giantId), legend to listOf(giantId)))

        val edges = driver.combatResolution().edges.filter {
            it.sourceId == giantId && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
        }
        edges.shouldNotBeEmpty()
        edges.forEach {
            it.editableBy shouldBe defender
            it.orderConstrained shouldBe false
        }
    }

    test("CR 702.22j: a bands-with-other blocker beside a creature without the quality changes nothing") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (giantId) = driver.creatures(active, "Test Giant")
        val (bander, gruntId) = driver.creatures(defender, "Test Legendary Bander", "Test Grunt")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(giantId), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(bander to listOf(giantId), gruntId to listOf(giantId)))

        val edges = driver.combatResolution().edges.filter {
            it.sourceId == giantId && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
        }
        edges.shouldNotBeEmpty()
        edges.forEach { it.editableBy shouldBe active }
    }

    test("CR 702.22k: the active player divides the damage of a creature blocking a bands-with-other band") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val band = driver.creatures(active, "Test Legendary Bander", "Test Legend A")
        val (giantId) = driver.creatures(defender, "Test Giant")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, band, defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // CR 702.22h: blocking one band member blocks the whole band.
        driver.declareBlockers(defender, mapOf(giantId to listOf(band.first())))

        val edges = driver.combatResolution().edges.filter { it.sourceId == giantId && !it.isTrampleDrain }
        edges.shouldNotBeEmpty()
        edges.forEach { it.editableBy shouldBe active }
    }
})
