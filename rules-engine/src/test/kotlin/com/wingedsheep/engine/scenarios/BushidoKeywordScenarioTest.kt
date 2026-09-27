package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine tests for bushido N (CR 702.45) as a keyword-derived trigger — printed on a card as the
 * bare [KeywordAbility.bushido], or granted by [Effects.GrantBushido] (Sensei Golden-Tail).
 *
 * | Rule | Covered by |
 * |---|---|
 * | 702.45a — blocks → +N/+N until end of turn | "blocking fires printed bushido" |
 * | 702.45a — becomes blocked → +N/+N | "becoming blocked fires printed bushido" |
 * | 702.45a — one trigger per combat, not per blocker | "blocked by two creatures triggers once" |
 * | 702.45a — nothing when unblocked | "an unblocked attacker gets nothing" |
 * | 702.45b — each instance triggers separately | "two printed instances trigger separately" |
 * | bushido is an ability — lose all abilities, no trigger | "a creature that lost its abilities has no bushido" |
 * | granted bushido triggers and counts for KeywordValue | "granted bushido triggers and is counted" |
 * | repeated grants add up (projected `<KEYWORD>_<n>` strings sum) | "two grants of bushido 1 are bushido 2" |
 * | the same summing fixes granted toxic (CR 702.164b total toxic value) | "two grants of toxic 1 are toxic 2" |
 */
class BushidoKeywordScenarioTest : FunSpec({

    val bushidoTwo = card("Test Bushido Two") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bushido(2))
    }

    val doubleBushido = card("Test Double Bushido") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.bushido(1))
        keywordAbility(KeywordAbility.bushido(2))
    }

    val train = card("Test Train") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantBushido(1, t, Duration.Permanent)
        }
    }

    val grantToxic = card("Test Grant Toxic") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantToxic(1, t)
        }
    }

    val strip = card("Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(bushidoTwo, doubleBushido, train, grantToxic, strip).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    val amounts = PredicateEvaluator(cardRegistry = null).amounts

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0
    fun GameTestDriver.keywordValue(id: EntityId, keyword: Keyword): Int = amounts.evaluate(
        state,
        DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.KeywordValue(keyword)),
        EffectContext(sourceId = id, controllerId = activePlayer!!)
    )

    /** Casts a one-target instant from the active player's hand and resolves it. */
    fun GameTestDriver.castAndResolve(name: String, target: EntityId, color: Color) {
        val caster = activePlayer!!
        val spell = putCardInHand(caster, name)
        giveMana(caster, color, 1)
        castSpell(caster, spell, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    /** The active player attacks with [attacker]; the opponent blocks with [blockers]. */
    fun GameTestDriver.attackInto(attacker: EntityId, blockers: List<EntityId>): Int {
        val me = activePlayer!!
        val opponent = getOpponent(me)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(attacker), opponent).error shouldBe null
        bothPass()
        declareBlockers(opponent, blockers.associateWith { listOf(attacker) }).error shouldBe null
        return state.stack.size
    }

    test("blocking fires printed bushido") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val samurai = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Test Bushido Two")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(bears), driver.getOpponent(me))
        driver.bothPass()
        driver.declareBlockers(driver.getOpponent(me), mapOf(samurai to listOf(bears)))
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.power(samurai) shouldBe 4
        driver.toughness(samurai) shouldBe 4
    }

    test("becoming blocked fires printed bushido") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Bushido Two")
        driver.removeSummoningSickness(samurai)
        val blocker = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")

        driver.attackInto(samurai, listOf(blocker)) shouldBe 1
        driver.bothPass()

        driver.power(samurai) shouldBe 4
        driver.toughness(samurai) shouldBe 4
    }

    test("blocked by two creatures triggers once") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Bushido Two")
        driver.removeSummoningSickness(samurai)
        val opponent = driver.getOpponent(me)
        val blockers = listOf(
            driver.putCreatureOnBattlefield(opponent, "Grizzly Bears"),
            driver.putCreatureOnBattlefield(opponent, "Grizzly Bears"),
        )

        withClue("one bushido trigger for the combat, not one per blocker") {
            driver.attackInto(samurai, blockers) shouldBe 1
        }
        driver.bothPass()
        driver.power(samurai) shouldBe 4
    }

    test("an unblocked attacker gets nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Bushido Two")
        driver.removeSummoningSickness(samurai)

        driver.attackInto(samurai, emptyList()) shouldBe 0
        driver.bothPass()
        driver.power(samurai) shouldBe 2
    }

    test("two printed instances trigger separately") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Double Bushido")
        driver.removeSummoningSickness(samurai)
        val blocker = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")

        withClue("bushido 1 and bushido 2 are two triggers (CR 702.45b)") {
            driver.attackInto(samurai, listOf(blocker)) shouldBe 2
        }
        driver.bothPass()
        driver.bothPass()
        driver.power(samurai) shouldBe 4
        driver.toughness(samurai) shouldBe 4
    }

    test("a creature that lost its abilities has no bushido") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Bushido Two")
        driver.removeSummoningSickness(samurai)
        val blocker = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")

        driver.castAndResolve("Test Strip", samurai, Color.BLUE)

        driver.attackInto(samurai, listOf(blocker)) shouldBe 0
        driver.power(samurai) shouldBe 2
    }

    test("granted bushido triggers and is counted") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val blocker = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")

        driver.castAndResolve("Test Train", bears, Color.WHITE)
        driver.keywordValue(bears, Keyword.BUSHIDO) shouldBe 1

        driver.attackInto(bears, listOf(blocker)) shouldBe 1
        driver.bothPass()
        driver.power(bears) shouldBe 3
        driver.toughness(bears) shouldBe 3
    }

    test("two grants of bushido 1 are bushido 2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val samurai = driver.putCreatureOnBattlefield(me, "Test Bushido Two")
        driver.removeSummoningSickness(samurai)
        val blocker = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Grizzly Bears")

        driver.castAndResolve("Test Train", samurai, Color.WHITE)
        driver.castAndResolve("Test Train", samurai, Color.WHITE)
        withClue("printed bushido 2 + granted 1 + granted 1") {
            driver.keywordValue(samurai, Keyword.BUSHIDO) shouldBe 4
        }

        withClue("the printed instance, plus one trigger for the granted total") {
            driver.attackInto(samurai, listOf(blocker)) shouldBe 2
        }
        driver.bothPass()
        driver.bothPass()
        driver.power(samurai) shouldBe 6
        driver.toughness(samurai) shouldBe 6
    }

    test("two grants of toxic 1 are toxic 2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.castAndResolve("Test Grant Toxic", bears, Color.BLACK)
        driver.castAndResolve("Test Grant Toxic", bears, Color.BLACK)

        driver.keywordValue(bears, Keyword.TOXIC) shouldBe 2
    }
})
