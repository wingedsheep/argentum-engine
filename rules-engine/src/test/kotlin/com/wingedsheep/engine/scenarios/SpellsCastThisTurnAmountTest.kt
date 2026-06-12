package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [DynamicAmount.SpellsCastThisTurn] — counts the spells a player has cast this turn,
 * optionally filtered (e.g. noncreature) and optionally excluding the resolving spell.
 *
 * Backs Thunder Salvo ("2 plus the number of *other* spells you've cast this turn") and
 * Magebane Lizard ("the number of noncreature spells they've cast this turn"). The first
 * group of tests drives the full cast pipeline through stand-in cards; the second pins the
 * counting/filter/exclude-self/per-player rules directly on the evaluator.
 */
class SpellsCastThisTurnAmountTest : FunSpec({

    // A 0/20 wall so spells can deal it damage without killing it — lets us read the amount
    // off its DamageComponent.
    val DamageSponge = card("Damage Sponge") {
        manaCost = "{0}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 20
        oracleText = ""
    }

    // Thunder Salvo stand-in: deals (2 + other spells you've cast this turn) to target creature.
    val OtherSpellsSalvo = card("Other Spells Salvo") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Deals damage to target creature equal to 2 plus the number of other spells you've cast this turn."
        spell {
            val t = target("target", TargetCreature())
            effect = DealDamageEffect(
                DynamicAmount.Add(
                    DynamicAmount.Fixed(2),
                    DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true)
                ),
                t
            )
        }
    }

    // Magebane Lizard stand-in: deals (noncreature spells you've cast this turn, including itself)
    // to target creature.
    val NoncreatureCount = card("Noncreature Count") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Deals damage to target creature equal to the number of noncreature spells you've cast this turn."
        spell {
            val t = target("target", TargetCreature())
            effect = DealDamageEffect(
                DynamicAmount.SpellsCastThisTurn(Player.You, GameObjectFilter.Noncreature),
                t
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + DamageSponge + OtherSpellsSalvo + NoncreatureCount)
        return driver
    }

    fun damageOn(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<DamageComponent>()?.amount ?: 0

    // =========================================================================
    // End-to-end through the cast pipeline
    // =========================================================================

    test("excludeSelf: salvo cast alone deals only its base 2 (no other spells)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val sponge = driver.putCreatureOnBattlefield(you, "Damage Sponge")

        val salvo = driver.putCardInHand(you, "Other Spells Salvo")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, salvo, listOf(sponge)).isSuccess shouldBe true
        driver.bothPass() // resolve the salvo

        // records = [salvo]; excludeSelf drops it → 0 other → damage = 2.
        damageOn(driver, sponge) shouldBe 2
    }

    test("excludeSelf: salvo counts other spells cast earlier this turn but not itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val sponge = driver.putCreatureOnBattlefield(you, "Damage Sponge")

        // Two prior spells this turn (left on the stack — the record is taken at cast time).
        val bolt1 = driver.putCardInHand(you, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(you, "Lightning Bolt")
        val salvo = driver.putCardInHand(you, "Other Spells Salvo")
        driver.giveMana(you, Color.RED, 3)

        driver.castSpell(you, bolt1, listOf(you)).isSuccess shouldBe true
        driver.castSpell(you, bolt2, listOf(you)).isSuccess shouldBe true
        driver.castSpell(you, salvo, listOf(sponge)).isSuccess shouldBe true
        driver.bothPass() // resolve the salvo (top of stack), bolts remain beneath

        // records = [bolt1, bolt2, salvo]; excludeSelf drops salvo → 2 others → damage = 2 + 2 = 4.
        damageOn(driver, sponge) shouldBe 4
    }

    test("filter (excludeSelf=false): noncreature count includes the resolving spell itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val sponge = driver.putCreatureOnBattlefield(you, "Damage Sponge")

        val counter = driver.putCardInHand(you, "Noncreature Count")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, counter, listOf(sponge)).isSuccess shouldBe true
        driver.bothPass()

        // records = [counter] (an instant = noncreature); includes itself → 1.
        damageOn(driver, sponge) shouldBe 1
    }

    test("filter: a prior creature spell is excluded; a prior noncreature spell is counted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val sponge = driver.putCreatureOnBattlefield(you, "Damage Sponge")

        val courser = driver.putCardInHand(you, "Centaur Courser") // creature spell — filtered out
        val bolt = driver.putCardInHand(you, "Lightning Bolt")     // noncreature — counted
        val counter = driver.putCardInHand(you, "Noncreature Count")
        driver.giveMana(you, Color.GREEN, 6)
        driver.giveMana(you, Color.RED, 6)

        driver.castSpell(you, courser, emptyList()).isSuccess shouldBe true
        driver.bothPass() // resolve the creature so it isn't left blocking the stack
        driver.castSpell(you, bolt, listOf(you)).isSuccess shouldBe true
        driver.castSpell(you, counter, listOf(sponge)).isSuccess shouldBe true
        driver.bothPass() // resolve Noncreature Count

        // noncreature records = [bolt, counter] (courser is a creature) → 2.
        damageOn(driver, sponge) shouldBe 2
    }

    // =========================================================================
    // Evaluator-level: per-player resolution, filter, and exclude-self matching
    // =========================================================================

    test("evaluator: counts per player and honors Player.You vs Player.EachOpponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.player1
        val opp = driver.player2
        val evaluator = DynamicAmountEvaluator()

        val instant = TypeLine.parse("Instant")
        val creature = TypeLine.parse("Creature")
        val state = driver.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                you to listOf(
                    CastSpellRecord(instant, 1, emptySet(), false, sourceEntityId = EntityId("1001")),
                    CastSpellRecord(creature, 2, emptySet(), false, sourceEntityId = EntityId("1002")),
                ),
                opp to listOf(
                    CastSpellRecord(instant, 1, emptySet(), false, sourceEntityId = EntityId("2001")),
                )
            )
        )
        val ctx = EffectContext(sourceId = null, controllerId = you)

        // Player.You, Any → both your spells.
        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.You), ctx) shouldBe 2
        // Player.You, Noncreature → only the instant.
        evaluator.evaluate(
            state,
            DynamicAmount.SpellsCastThisTurn(Player.You, GameObjectFilter.Noncreature),
            ctx
        ) shouldBe 1
        // Player.EachOpponent → the single opponent spell.
        evaluator.evaluate(state, DynamicAmount.SpellsCastThisTurn(Player.EachOpponent), ctx) shouldBe 1
    }

    test("evaluator: excludeSelf drops the record whose sourceEntityId matches the source") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.player1
        val evaluator = DynamicAmountEvaluator()

        val instant = TypeLine.parse("Instant")
        val state = driver.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                you to listOf(
                    CastSpellRecord(instant, 1, emptySet(), false, sourceEntityId = EntityId("1001")),
                    CastSpellRecord(instant, 1, emptySet(), false, sourceEntityId = EntityId("1002")),
                    CastSpellRecord(instant, 1, emptySet(), false, sourceEntityId = EntityId("1003")),
                )
            )
        )

        // Source is the spell with id 1003 (the resolving one) → excludeSelf counts the other 2.
        val ctxSelf = EffectContext(sourceId = EntityId("1003"), controllerId = you)
        evaluator.evaluate(
            state,
            DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true),
            ctxSelf
        ) shouldBe 2

        // Without excludeSelf, all 3 count regardless of source.
        evaluator.evaluate(
            state,
            DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = false),
            ctxSelf
        ) shouldBe 3

        // excludeSelf with a source that isn't in the list drops nothing.
        val ctxOther = EffectContext(sourceId = EntityId("9999"), controllerId = you)
        evaluator.evaluate(
            state,
            DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true),
            ctxOther
        ) shouldBe 3
    }

    test("description reads as oracle-style text") {
        DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true).description shouldBe
            "the number of other spells you've cast this turn"
        DynamicAmount.SpellsCastThisTurn(Player.You, GameObjectFilter.Noncreature).description shouldBe
            "the number of noncreature spells you've cast this turn"
    }
})
