package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Infect (CR 702.90):
 *  - damage to a player gives that player that many poison counters instead of life loss
 *    (CR 702.90b / 120.3b) — combat and noncombat alike;
 *  - damage to a creature is dealt as -1/-1 counters, never marked (CR 702.90c / 120.3d);
 *  - it is still damage, so lifelink still gains life (CR 120.3f);
 *  - ten poison counters lose the game (CR 704.5c).
 */
class InfectTest : FunSpec({

    val infectBear = CardDefinition.creature(
        name = "Infect Bear",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = "Infect",
        keywords = setOf(Keyword.INFECT)
    )

    val infectLifelinker = CardDefinition.creature(
        name = "Infect Lifelinker",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Bat")),
        power = 3,
        toughness = 3,
        oracleText = "Infect, lifelink",
        keywords = setOf(Keyword.INFECT, Keyword.LIFELINK)
    )

    val infectTitan = CardDefinition.creature(
        name = "Infect Titan",
        manaCost = ManaCost.parse("{8}"),
        subtypes = setOf(Subtype("Horror")),
        power = 10,
        toughness = 10,
        oracleText = "Infect",
        keywords = setOf(Keyword.INFECT)
    )

    val infectPinger = card("Infect Pinger") {
        manaCost = "{B}"
        typeLine = "Creature — Phyrexian Wizard"
        power = 1
        toughness = 1
        oracleText = "Infect\n{T}: This creature deals 2 damage to any target."
        keywords(Keyword.INFECT)
        activatedAbility {
            cost = Costs.Tap
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }

    val infectFanatic = card("Infect Fanatic") {
        manaCost = "{B}"
        typeLine = "Creature — Phyrexian Goblin"
        power = 1
        toughness = 1
        oracleText = "Infect\nSacrifice this creature: It deals 2 damage to any target."
        keywords(Keyword.INFECT)
        activatedAbility {
            cost = Costs.SacrificeSelf
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }

    val toxicInfector = card("Toxic Infector") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Phyrexian Horror"
        power = 1
        toughness = 1
        oracleText = "Infect\nToxic 2"
        keywords(Keyword.INFECT)
        keywordAbility(KeywordAbility.toxic(2))
    }

    val infectSpellAnthem = card("Infect Spell Anthem") {
        manaCost = "{2}{B}"
        typeLine = "Enchantment"
        oracleText = "Instant and sorcery spells you control have infect."
        staticAbility {
            ability = GrantKeywordToOwnSpells(Keyword.INFECT, spellFilter = GameObjectFilter.InstantOrSorcery)
        }
    }

    val bigBlocker = CardDefinition.creature(
        name = "Big Blocker",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 1,
        toughness = 4,
        oracleText = ""
    )

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(infectBear, infectLifelinker, infectTitan, infectPinger, infectFanatic, toxicInfector, infectSpellAnthem, bigBlocker))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.counters(entity: EntityId, type: CounterType): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.attackUnblocked(attacker: EntityId, attackerOwner: EntityId, defender: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attackerOwner, listOf(attacker), defender)
        bothPass()
        declareNoBlockers(defender)
        bothPass()
        bothPass()
    }

    test("combat damage to a player gives poison counters instead of life loss") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bear = driver.putCreatureOnBattlefield(me, "Infect Bear")
        driver.removeSummoningSickness(bear)

        driver.attackUnblocked(bear, me, opponent)

        driver.counters(opponent, CounterType.POISON) shouldBe 2
        driver.assertLifeTotal(opponent, 20)
        // No life was lost, so "lost life this turn" stays unset.
        driver.state.getEntity(opponent)?.has<LifeLostThisTurnComponent>() shouldBe false
    }

    test("combat damage to a creature is dealt as -1/-1 counters, not marked damage") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bear = driver.putCreatureOnBattlefield(me, "Infect Bear")
        driver.removeSummoningSickness(bear)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Big Blocker")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(bear), opponent)
        driver.bothPass()
        driver.declareBlockers(opponent, mapOf(blocker to listOf(bear)))
        driver.bothPass()
        driver.bothPass()
        driver.bothPass()

        driver.counters(blocker, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
        (driver.state.getEntity(blocker)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        driver.assertPermanentExists(opponent, "Big Blocker")
        // The blocker has no infect: the damage it deals back is ordinary marked damage.
        driver.counters(bear, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
    }

    test("lifelink still gains life from infect damage dealt to a player") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val lifelinker = driver.putCreatureOnBattlefield(me, "Infect Lifelinker")
        driver.removeSummoningSickness(lifelinker)

        driver.attackUnblocked(lifelinker, me, opponent)

        driver.counters(opponent, CounterType.POISON) shouldBe 3
        driver.assertLifeTotal(opponent, 20)
        driver.assertLifeTotal(me, 23)
    }

    test("ten poison counters from infect damage lose the game") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val titan = driver.putCreatureOnBattlefield(me, "Infect Titan")
        driver.removeSummoningSickness(titan)

        driver.attackUnblocked(titan, me, opponent)

        driver.assertGameOver(expectedWinner = me)
    }

    test("noncombat damage from an infect source gives poison to a player and -1/-1 counters to a creature") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val pinger = driver.putCreatureOnBattlefield(me, "Infect Pinger")
        driver.removeSummoningSickness(pinger)
        val pinger2 = driver.putCreatureOnBattlefield(me, "Infect Pinger")
        driver.removeSummoningSickness(pinger2)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Big Blocker")
        val abilityId = infectPinger.script.activatedAbilities.single().id

        driver.submit(ActivateAbility(me, pinger, abilityId, targets = listOf(ChosenTarget.Player(opponent))))
        driver.bothPass()
        driver.counters(opponent, CounterType.POISON) shouldBe 2
        driver.assertLifeTotal(opponent, 20)

        driver.submit(ActivateAbility(me, pinger2, abilityId, targets = listOf(ChosenTarget.Permanent(blocker))))
        driver.bothPass()
        driver.counters(blocker, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
        (driver.state.getEntity(blocker)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("a source that left the battlefield uses its last-known infect (CR 702.90d)") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val fanatic = driver.putCreatureOnBattlefield(me, "Infect Fanatic")
        val abilityId = infectFanatic.script.activatedAbilities.single().id

        driver.submit(ActivateAbility(me, fanatic, abilityId, targets = listOf(ChosenTarget.Player(opponent))))
        driver.bothPass()

        driver.counters(opponent, CounterType.POISON) shouldBe 2
        driver.assertLifeTotal(opponent, 20)
    }

    test("a spell granted infect deals its damage as poison (CR 702.90e)") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Infect Spell Anthem")
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.RED, 1)

        driver.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.counters(opponent, CounterType.POISON) shouldBe 3
        driver.assertLifeTotal(opponent, 20)
    }

    test("infect and toxic on one creature give their poison together") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val infector = driver.putCreatureOnBattlefield(me, "Toxic Infector")
        driver.removeSummoningSickness(infector)

        driver.attackUnblocked(infector, me, opponent)

        // 1 from infect's damage (CR 120.3b) + 2 from toxic (CR 120.3g).
        driver.counters(opponent, CounterType.POISON) shouldBe 3
        driver.assertLifeTotal(opponent, 20)
    }
})
