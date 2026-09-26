package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for [EntityNumericProperty.KeywordValue] — "for each point of bushido it has"
 * (Takeno, Samurai General). The amount is read per affected creature inside the layer projection,
 * which has no card registry, so the printed N has to travel on the entity.
 *
 * | Behaviour | Covered by |
 * |---|---|
 * | reads the printed N, per affected creature | "each Samurai gets +N/+N for its own bushido N" |
 * | multiple instances add (a creature's bushido values are summed) | "two instances of bushido add" |
 * | no bushido counts 0 | "a Samurai without bushido gets nothing" |
 * | a keyword lost in layer 6 counts 0 (the value is an ability's) | "losing all abilities drops the bonus" |
 * | numeric keywords projected as `<KEYWORD>_<n>` (granted toxic) add their N | "granted toxic adds to printed toxic" |
 * | off the battlefield the printed value is read directly | "a card in hand reads its printed value" |
 */
class KeywordValueAmountScenarioTest : ScenarioTestBase() {

    private val samuraiLord = card("Test Bushido Lord") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        staticAbility {
            val bonus = DynamicAmounts.propertyOf(
                EffectTarget.AffectedEntity,
                EntityNumericProperty.KeywordValue(Keyword.BUSHIDO)
            )
            ability = GrantDynamicStats(
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SAMURAI).youControl()).other(),
                powerBonus = bonus,
                toughnessBonus = bonus
            )
        }
    }

    private val bushidoTwo = card("Test Bushido Two") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.bushido(2))
    }

    private val doubleBushido = card("Test Double Bushido") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.bushido(1))
        keywordAbility(KeywordAbility.bushido(2))
    }

    private val plainSamurai = card("Test Plain Samurai") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 2
        toughness = 2
    }

    private val toxicTwo = card("Test Toxic Two") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Phyrexian Rat"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.toxic(2))
    }

    private val strip = card("Test Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
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

    private val amounts = PredicateEvaluator(cardRegistry = null).amounts

    private fun TestGame.keywordValue(id: EntityId, keyword: Keyword): Int = amounts.evaluate(
        state,
        DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.KeywordValue(keyword)),
        EffectContext(sourceId = id, controllerId = player1Id)
    )

    init {
        listOf(samuraiLord, bushidoTwo, doubleBushido, plainSamurai, toxicTwo, strip, grantToxic)
            .forEach { cardRegistry.register(it) }

        context("KeywordValue fed to a per-creature static") {

            test("each Samurai gets +N/+N for its own bushido N") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Test Bushido Lord")
                    .withCardOnBattlefield(1, "Test Bushido Two")
                    .withCardOnBattlefield(2, "Test Bushido Two")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (mine, theirs) = game.findPermanents("Test Bushido Two")
                    .partition { game.state.projectedState.getController(it) == game.player1Id }
                    .let { (a, b) -> a.single() to b.single() }
                withClue("bushido 2 → +2/+2") {
                    game.state.projectedState.getPower(mine) shouldBe 3
                    game.state.projectedState.getToughness(mine) shouldBe 3
                }
                withClue("the opponent's Samurai isn't one you control") {
                    game.state.projectedState.getPower(theirs) shouldBe 1
                }
            }

            test("two instances of bushido add") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Test Bushido Lord")
                    .withCardOnBattlefield(1, "Test Double Bushido")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val samurai = game.findPermanent("Test Double Bushido")!!
                withClue("bushido 1 + bushido 2 = three points → +3/+3") {
                    game.state.projectedState.getPower(samurai) shouldBe 4
                    game.state.projectedState.getToughness(samurai) shouldBe 4
                }
            }

            test("a Samurai without bushido gets nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Test Bushido Lord")
                    .withCardOnBattlefield(1, "Test Plain Samurai")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val samurai = game.findPermanent("Test Plain Samurai")!!
                game.state.projectedState.getPower(samurai) shouldBe 2
                game.state.projectedState.getToughness(samurai) shouldBe 2
            }

            test("losing all abilities drops the bonus — bushido is an ability") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Test Bushido Lord")
                    .withCardOnBattlefield(1, "Test Bushido Two")
                    .withCardInHand(1, "Test Strip")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val samurai = game.findPermanent("Test Bushido Two")!!
                game.state.projectedState.getPower(samurai) shouldBe 3

                game.castSpell(1, "Test Strip", targetId = samurai).error shouldBe null
                game.resolveStack()

                withClue("it's still a Samurai, but has no bushido left to count") {
                    game.keywordValue(samurai, Keyword.BUSHIDO) shouldBe 0
                    game.state.projectedState.getPower(samurai) shouldBe 1
                    game.state.projectedState.getToughness(samurai) shouldBe 1
                }
            }
        }

        context("KeywordValue read directly") {

            test("granted toxic adds to printed toxic") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Test Toxic Two")
                    .withCardInHand(1, "Test Grant Toxic")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rat = game.findPermanent("Test Toxic Two")!!
                game.keywordValue(rat, Keyword.TOXIC) shouldBe 2

                game.castSpell(1, "Test Grant Toxic", targetId = rat).error shouldBe null
                game.resolveStack()

                game.keywordValue(rat, Keyword.TOXIC) shouldBe 3
            }

            test("a card in hand reads its printed value") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Test Double Bushido")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val card = game.findCardsInHand(1, "Test Double Bushido").single()
                game.keywordValue(card, Keyword.BUSHIDO) shouldBe 3
                game.keywordValue(card, Keyword.TOXIC) shouldBe 0
            }

            test("a toxic card in hand reads its printed toxic") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Test Toxic Two")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val card = game.findCardsInHand(1, "Test Toxic Two").single()
                game.keywordValue(card, Keyword.TOXIC) shouldBe 2
            }
        }
    }
}
