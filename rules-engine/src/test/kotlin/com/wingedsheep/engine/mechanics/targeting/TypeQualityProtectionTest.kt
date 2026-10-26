package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection from a type quality narrowed to a card type (CR 702.16a: the quality can be any
 * characteristic) — "protection from God creatures" (Sigrid, God-Favored) and "protection from
 * legendary creatures" (Tsabo Tavoc). The source must have the subtype/supertype *and* the card
 * type, so a God that isn't a creature, or a legendary artifact, is not a quality the permanent is
 * protected from. Targeting (CR 702.16b), damage (702.16e) and blocking (702.16f) are pinned, plus
 * the un-narrowed "protection from Gods" for contrast.
 */
class TypeQualityProtectionTest : FunSpec({

    fun pinger(name: String, typeLine: String, creature: Boolean) = card(name) {
        manaCost = "{1}"
        this.typeLine = typeLine
        if (creature) {
            power = 3
            toughness = 3
        }
        activatedAbility {
            cost = Costs.Tap
            val victim = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, victim)
            description = "{T}: Deal 1 damage to target creature."
        }
    }

    val godCreature = pinger("Test God Creature", "Legendary Creature — God", creature = true)
    val godEnchantment = pinger("Test God Enchantment", "Legendary Enchantment — God", creature = false)
    val legendaryArtifact = pinger("Test Legendary Relic", "Legendary Artifact", creature = false)
    val legendaryCreature = pinger("Test Legendary Hero", "Legendary Creature — Human", creature = true)

    fun protectedCreature(name: String, ability: KeywordAbility) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 1
        keywordAbility(ability)
    }

    val fromGodCreatures = protectedCreature("Pro God Creatures", KeywordAbility.protectionFromSubtype("God", "Creature"))
    val fromGods = protectedCreature("Pro Gods", KeywordAbility.protectionFromSubtype("God"))
    val fromLegendaryCreatures =
        protectedCreature("Pro Legendary Creatures", KeywordAbility.protectionFromSupertype("Legendary", "Creature"))

    val testCards: List<CardDefinition> = listOf(
        godCreature, godEnchantment, legendaryArtifact, legendaryCreature,
        fromGodCreatures, fromGods, fromLegendaryCreatures,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + testCards)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.ping(controller: EntityId, sourceId: EntityId, source: CardDefinition, target: EntityId) =
        submit(
            ActivateAbility(
                playerId = controller,
                sourceId = sourceId,
                abilityId = source.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(target)),
            )
        )

    test("the narrowed quality projects as its own keyword") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sigrid = driver.putCreatureOnBattlefield(you, "Pro God Creatures")
        val tsabo = driver.putCreatureOnBattlefield(you, "Pro Legendary Creatures")
        driver.state.projectedState.getKeywords(sigrid) shouldContain "PROTECTION_FROM_SUBTYPE_GOD@CREATURE"
        driver.state.projectedState.getKeywords(tsabo) shouldContain "PROTECTION_FROM_SUPERTYPE_LEGENDARY@CREATURE"
    }

    test("a God creature's ability can't target it, a noncreature God's can") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val protected = driver.putCreatureOnBattlefield(opp, "Pro God Creatures")
        val creatureGod = driver.putCreatureOnBattlefield(you, "Test God Creature")
        driver.removeSummoningSickness(creatureGod)
        val enchantmentGod = driver.putPermanentOnBattlefield(you, "Test God Enchantment")

        driver.ping(you, creatureGod, godCreature, protected).outcome shouldNotBe Outcome.Done

        driver.ping(you, enchantmentGod, godEnchantment, protected).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getBattlefield() shouldNotContain protected
    }

    test("un-narrowed protection from Gods also stops a noncreature God") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val protected = driver.putCreatureOnBattlefield(opp, "Pro Gods")
        val enchantmentGod = driver.putPermanentOnBattlefield(you, "Test God Enchantment")

        driver.ping(you, enchantmentGod, godEnchantment, protected).outcome shouldNotBe Outcome.Done
    }

    test("a God creature can't block it; another creature can") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val attacker = driver.putCreatureOnBattlefield(you, "Pro God Creatures")
        driver.removeSummoningSickness(attacker)
        val creatureGod = driver.putCreatureOnBattlefield(opp, "Test God Creature")
        val hero = driver.putCreatureOnBattlefield(opp, "Test Legendary Hero")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(attacker), opp)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(opp, mapOf(creatureGod to listOf(attacker))).outcome shouldNotBe Outcome.Done
        driver.declareBlockers(opp, mapOf(hero to listOf(attacker))).outcome shouldBe Outcome.Done
    }

    test("combat damage from a God creature is prevented") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val creatureGod = driver.putCreatureOnBattlefield(you, "Test God Creature")
        driver.removeSummoningSickness(creatureGod)
        val blocker = driver.putCreatureOnBattlefield(opp, "Pro God Creatures")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(creatureGod), opp)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opp, mapOf(blocker to listOf(creatureGod))).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.state.getBattlefield() shouldContain blocker
    }

    test("protection from legendary creatures doesn't stop a legendary artifact") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val protected = driver.putCreatureOnBattlefield(opp, "Pro Legendary Creatures")
        val hero = driver.putCreatureOnBattlefield(you, "Test Legendary Hero")
        driver.removeSummoningSickness(hero)
        val relic = driver.putPermanentOnBattlefield(you, "Test Legendary Relic")

        driver.ping(you, hero, legendaryCreature, protected).outcome shouldNotBe Outcome.Done

        driver.ping(you, relic, legendaryArtifact, protected).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getBattlefield() shouldNotContain protected
    }
})
