package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Unified Strike.
 *
 * Unified Strike: {W}
 * Instant
 * Exile target attacking creature if its power is less than or equal to
 * the number of Soldiers on the battlefield.
 */
class UnifiedStrikeTest : FunSpec({

    val UnifiedStrike = CardDefinition.instant(
        name = "Unified Strike",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "Exile target attacking creature if its power is less than or equal to the number of Soldiers on the battlefield.",
        script = CardScript.spell(
            effect = ConditionalEffect(
                condition = Conditions.TargetPowerAtMost(
                    DynamicAmounts.creaturesWithSubtype(Subtype("Soldier"))
                ),
                effect = MoveToZoneEffect(EffectTarget.BoundVariable("target"), Zone.EXILE)
            ),
            TargetCreature(id = "target", filter = TargetFilter.AttackingCreature)
        )
    )

    val TestSoldier = CardDefinition.creature(
        name = "Test Soldier",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 1,
        toughness = 1,
        oracleText = "Test Soldier for Unified Strike tests."
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UnifiedStrike, TestSoldier))
        return driver
    }

    test("exiles attacking creature when its power equals the number of Soldiers") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has 2 Soldiers on the battlefield
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")

        // Active player attacks with Grizzly Bears (2/2)
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(opponent)

        // Opponent casts Unified Strike targeting the attacker (power 2 <= 2 Soldiers)
        val unifiedStrike = driver.putCardInHand(opponent, "Unified Strike")
        driver.giveMana(opponent, Color.WHITE, 1)

        // Active player passes, opponent casts
        driver.passPriority(activePlayer)
        val castResult = driver.castSpell(opponent, unifiedStrike, listOf(attacker))
        castResult.isSuccess shouldBe true

        // Let it resolve
        driver.bothPass()

        // Attacker should be exiled
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        val exileZone = ZoneKey(activePlayer, Zone.EXILE)
        val exiledNames = driver.state.getZone(exileZone).mapNotNull { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name
        }
        exiledNames shouldContain "Grizzly Bears"
    }

    test("does not exile attacking creature when its power exceeds the number of Soldiers") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has only 1 Soldier on the battlefield
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")

        // Active player attacks with Grizzly Bears (2/2) — power 2 > 1 Soldier
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        driver.declareNoBlockers(opponent)

        val unifiedStrike = driver.putCardInHand(opponent, "Unified Strike")
        driver.giveMana(opponent, Color.WHITE, 1)

        driver.passPriority(activePlayer)
        val castResult = driver.castSpell(opponent, unifiedStrike, listOf(attacker))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // Attacker should still be on the battlefield (power 2 > 1 Soldier)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null
    }

    test("exiles creature when power is less than the number of Soldiers") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has 3 Soldiers on the battlefield
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")

        // Active player attacks with Grizzly Bears (2/2) — power 2 < 3 Soldiers
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        driver.declareNoBlockers(opponent)

        val unifiedStrike = driver.putCardInHand(opponent, "Unified Strike")
        driver.giveMana(opponent, Color.WHITE, 1)

        driver.passPriority(activePlayer)
        driver.castSpell(opponent, unifiedStrike, listOf(attacker))
        driver.bothPass()

        // Attacker should be exiled
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        val exileZone = ZoneKey(activePlayer, Zone.EXILE)
        val exiledNames = driver.state.getZone(exileZone).mapNotNull { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name
        }
        exiledNames shouldContain "Grizzly Bears"
    }

    test("counts Soldiers from all players") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 1 Soldier on each side = 2 total Soldiers
        driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        driver.putCreatureOnBattlefield(opponent, "Test Soldier")

        // Active player attacks with Grizzly Bears (2/2) — power 2 <= 2 total Soldiers
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        driver.declareNoBlockers(opponent)

        val unifiedStrike = driver.putCardInHand(opponent, "Unified Strike")
        driver.giveMana(opponent, Color.WHITE, 1)

        driver.passPriority(activePlayer)
        driver.castSpell(opponent, unifiedStrike, listOf(attacker))
        driver.bothPass()

        // Should be exiled because 2 total Soldiers >= 2 power
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
    }

    test("does not exile when there are zero Soldiers") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No Soldiers on the battlefield
        // Active player attacks with Grizzly Bears (2/2) — power 2 > 0 Soldiers
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent)
        driver.bothPass()

        driver.declareNoBlockers(opponent)

        val unifiedStrike = driver.putCardInHand(opponent, "Unified Strike")
        driver.giveMana(opponent, Color.WHITE, 1)

        driver.passPriority(activePlayer)
        driver.castSpell(opponent, unifiedStrike, listOf(attacker))
        driver.bothPass()

        // Should still be on the battlefield
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null
    }
})
