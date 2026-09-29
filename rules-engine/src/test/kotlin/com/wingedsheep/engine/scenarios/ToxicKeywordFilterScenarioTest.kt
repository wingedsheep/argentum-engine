package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.containsKeyword
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "A creature with toxic" (CR 702.164) — Phyrexia: All Will Be One's toxic payoffs (Slaughter
 * Singer, Compleat Devotion, Hexgold Slash) ask whether a creature has the keyword at all, not for
 * any particular N. Toxic never projects as a bare `TOXIC`: printed toxic 2 is `TOXIC_2`, and each
 * "gains toxic 1" grant adds its own `TOXIC_1`. So `withKeyword(TOXIC)` has to read the numeric
 * form, on every filter path — the evaluator, static-ability group filters, and trigger subjects.
 *
 * | Behaviour | Covered by |
 * |---|---|
 * | the numeric `<KEYWORD>_<n>` form answers the bare keyword; other suffixes don't | "keyword-string matching" |
 * | printed toxic matches `withKeyword(TOXIC)`, a creature without doesn't | "printed toxic" |
 * | a granted toxic makes a creature match | "granted toxic" |
 * | losing all abilities (layer 6) stops it matching | "losing all abilities" |
 * | a static's group filter ("creatures you control with toxic") sees printed toxic | "a lord over creatures with toxic" |
 * | an inline token created "with toxic 1" has printed toxic | "an inline token with toxic 1" |
 */
class ToxicKeywordFilterScenarioTest : ScenarioTestBase() {

    private val toxicTwo = card("Test Toxic Two") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Phyrexian Rat"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.toxic(2))
    }

    private val plain = card("Test Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val toxicLord = card("Test Toxic Lord") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Phyrexian Cleric"
        power = 1
        toughness = 1
        staticAbility {
            ability = ModifyStats(
                1, 1,
                GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.TOXIC).youControl()).other()
            )
        }
    }

    private val grantToxic = card("Test Grant Toxic") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantToxic(1, t)
        }
    }

    private val strip = card("Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    private val beastMaker = card("Test Beast Maker") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                creatureTypes = setOf("Phyrexian", "Beast"),
                numericKeywords = listOf(KeywordAbility.toxic(1)),
            )
        }
    }

    private val withToxic = GameObjectFilter.Creature.withKeyword(Keyword.TOXIC)
    private val withoutToxic = GameObjectFilter.Creature.withoutKeyword(Keyword.TOXIC)

    private val predicates = PredicateEvaluator(cardRegistry = null)

    private fun TestGame.matches(id: EntityId, filter: GameObjectFilter): Boolean =
        predicates.matches(state, state.projectedState, id, filter, PredicateContext(controllerId = player1Id))

    init {
        listOf(toxicTwo, plain, toxicLord, grantToxic, strip, beastMaker).forEach { cardRegistry.register(it) }

        test("keyword-string matching: TOXIC_<n> is toxic, a non-numeric suffix is not") {
            setOf("TOXIC_2").containsKeyword(Keyword.TOXIC) shouldBe true
            setOf("TOXIC_1", "TOXIC_1x").containsKeyword(Keyword.TOXIC) shouldBe true
            setOf("TOXIC_").containsKeyword(Keyword.TOXIC) shouldBe false
            setOf("FLYING").containsKeyword(Keyword.TOXIC) shouldBe false
            setOf("PROTECTION_FROM_RED").containsKeyword(Keyword.PROTECTION) shouldBe false
            setOf("FLYING").containsKeyword(Keyword.FLYING) shouldBe true
        }

        test("printed toxic matches withKeyword(TOXIC); a creature without toxic doesn't") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Test Toxic Two")
                .withCardOnBattlefield(1, "Test Plain Bear")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val rat = game.findPermanent("Test Toxic Two")!!
            val bear = game.findPermanent("Test Plain Bear")!!
            game.matches(rat, withToxic) shouldBe true
            game.matches(rat, withoutToxic) shouldBe false
            game.matches(bear, withToxic) shouldBe false
            game.matches(bear, withoutToxic) shouldBe true
            game.state.projectedState.hasKeyword(rat, Keyword.TOXIC) shouldBe true
        }

        test("granted toxic makes a creature without it match") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Test Plain Bear")
                .withCardInHand(1, "Test Grant Toxic")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Test Plain Bear")!!
            game.matches(bear, withToxic) shouldBe false
            game.castSpell(1, "Test Grant Toxic", targetId = bear).error shouldBe null
            game.resolveStack()
            withClue("gains toxic 1 → TOXIC_1 → has toxic") {
                game.matches(bear, withToxic) shouldBe true
            }
        }

        test("losing all abilities stops a printed-toxic creature matching") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Test Toxic Two")
                .withCardInHand(1, "Test Strip")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val rat = game.findPermanent("Test Toxic Two")!!
            game.castSpell(1, "Test Strip", targetId = rat).error shouldBe null
            game.resolveStack()
            game.matches(rat, withToxic) shouldBe false
        }

        test("a lord over \"other creatures you control with toxic\" pumps only those") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Test Toxic Lord")
                .withCardOnBattlefield(1, "Test Toxic Two")
                .withCardOnBattlefield(1, "Test Plain Bear")
                .withCardOnBattlefield(2, "Test Toxic Two")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val (mine, theirs) = game.findPermanents("Test Toxic Two")
                .partition { projected.getController(it) == game.player1Id }
                .let { (a, b) -> a.single() to b.single() }
            projected.getPower(mine) shouldBe 2
            projected.getPower(theirs) shouldBe 1
            projected.getPower(game.findPermanent("Test Plain Bear")!!) shouldBe 2
        }

        test("an inline token created \"with toxic 1\" has printed toxic 1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Test Beast Maker")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Beast Maker").error shouldBe null
            game.resolveStack()

            val beast = game.findPermanents("Phyrexian Beast Token").singleOrNull()
                ?: game.state.getBattlefield().single { game.state.projectedState.isCreature(it) }
            game.state.getEntity(beast)!!.get<ToxicComponent>() shouldNotBe null
            game.state.getEntity(beast)!!.get<ToxicComponent>()!!.amount shouldBe 1
            game.state.projectedState.getKeywords(beast).contains("TOXIC_1") shouldBe true
            game.matches(beast, withToxic) shouldBe true
        }
    }
}
