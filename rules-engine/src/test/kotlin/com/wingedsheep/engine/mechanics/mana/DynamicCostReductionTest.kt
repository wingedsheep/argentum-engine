package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for [CostReductionSource.Dynamic] — "spells you cast cost {X} less to cast, where X
 * is `<DynamicAmount>`", evaluated with the permanent that carries the modifier as the amount's
 * `EffectTarget.Self`.
 *
 * Named for the primitive rather than a card because the point of the shape is that the amount is
 * read from the permanent carrying the modifier, not aggregated over a board. The cases below are
 * the ones a group-aggregate approximation
 * ([CostReductionSource.GreatestPropertyAmongPermanentsYouControl] over a name filter) gets wrong:
 * two sources each discount by their *own* value, and the biggest creature you control is
 * irrelevant unless it is itself a source.
 *
 * [com.wingedsheep.engine.scenarios.TheScarletWitchScenarioTest] covers the printed card.
 */
class DynamicCostReductionTest : ScenarioTestBase() {

    /** "Spells you cast cost {X} less to cast, where X is this creature's power." 2/3. */
    private val smallDiscounter = card("Test Small Discounter") {
        manaCost = "{2}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Wizard"
        power = 2
        toughness = 3
        oracleText = "Spells you cast cost {X} less to cast, where X is this creature's power."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
                ),
            )
        }
    }

    /** The same static on a 5/5, so the two sources can be told apart by their contribution. */
    private val bigDiscounter = card("Test Big Discounter") {
        manaCost = "{4}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Wizard"
        power = 5
        toughness = 5
        oracleText = "Spells you cast cost {X} less to cast, where X is this creature's power."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
                ),
            )
        }
    }

    /** A 0/4 whose reduction reads *toughness*, proving the property axis is honored. */
    private val toughnessDiscounter = card("Test Toughness Discounter") {
        manaCost = "{3}"
        colorIdentity = ""
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
        oracleText = "Spells you cast cost {X} less to cast, where X is this creature's toughness."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.sourceToughness())
                ),
            )
        }
    }

    /**
     * The counter-counting shape the KDoc advertises — "where X is the number of charge counters on
     * this artifact". Reads a property the *group* aggregates cannot: no P/T involved at all.
     */
    private val counterDiscounter = card("Test Counter Discounter") {
        manaCost = "{2}"
        colorIdentity = ""
        typeLine = "Artifact"
        oracleText = "Spells you cast cost {X} less to cast, where X is the number of charge " +
            "counters on this artifact."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(
                        DynamicAmounts.countersOnSelf(CounterType.CHARGE)
                    )
                ),
            )
        }
    }

    /** A global tax-shaped source: *every* player's spells read this permanent's power. */
    private val anyCasterDiscounter = card("Test Global Discounter") {
        manaCost = "{3}"
        colorIdentity = ""
        typeLine = "Creature — Construct"
        power = 3
        toughness = 3
        oracleText = "Spells cost {X} less to cast, where X is this creature's power."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.AnyCaster(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
                ),
            )
        }
    }

    /**
     * The nonsensical-but-expressible combination: a self-cast reduction reading a source
     * permanent that doesn't exist, because the card *is* the spell. Must contribute 0 rather
     * than reading anything off the stack or the battlefield.
     */
    private val selfCastDiscounter = card("Test Self Discounter") {
        manaCost = "{6}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Giant"
        power = 7
        toughness = 7
        oracleText = "This spell costs {X} less to cast, where X is this creature's power."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
                ),
            )
        }
    }

    /**
     * A self-cast reduction over a *player-scoped* amount — Bloodsoaked Insight's shape. "You" is
     * the caster, so "your opponents" are the caster's opponents.
     */
    private val selfCastPlayerDiscounter = card("Test Self Insight") {
        manaCost = "{6}{B}"
        colorIdentity = "B"
        typeLine = "Sorcery"
        oracleText = "This spell costs {1} less to cast for each 1 life your opponents have lost this turn.\nDraw a card."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.Dynamic(DynamicAmounts.lifeLostThisTurn(Player.EachOpponent))
                ),
            )
        }
        spell {
            effect = Effects.DrawCards(1)
        }
    }

    /** Shrinks every creature you control by 4 power, so a 2/3 source goes to −2 power. */
    private val witheringField = card("Test Withering Field") {
        manaCost = "{2}{B}"
        colorIdentity = "B"
        typeLine = "Enchantment"
        oracleText = "Creatures you control get -4/-0."
        staticAbility {
            ability = ModifyStats(
                powerBonus = -4,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    /** {5}{R} instant — the spell being priced in every test below. */
    private val pricedSpell = card("Test Priced Spell") {
        manaCost = "{5}{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell {
            effect = Effects.DrawCards(1)
        }
    }

    private fun costOfPricedSpell(game: TestGame, playerNumber: Int) =
        CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator).calculateEffectiveCost(
            game.state,
            cardRegistry.requireCard("Test Priced Spell"),
            if (playerNumber == 1) game.player1Id else game.player2Id,
        )

    private fun genericCostOfPricedSpell(game: TestGame, playerNumber: Int): Int =
        costOfPricedSpell(game, playerNumber).genericAmount

    init {
        cardRegistry.register(
            listOf(
                smallDiscounter,
                bigDiscounter,
                toughnessDiscounter,
                counterDiscounter,
                anyCasterDiscounter,
                selfCastDiscounter,
                selfCastPlayerDiscounter,
                witheringField,
                pricedSpell,
            )
        )

        context("CostReductionSource.Dynamic over the source permanent") {

            test("reduces by the source permanent's own power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("a 2/3 source shaves {2} off the {5} generic") {
                    genericCostOfPricedSpell(game, 1) shouldBe 3
                }
            }

            test("two sources each contribute their own value, not the greatest among them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withCardOnBattlefield(1, "Test Big Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("2 + 5 = 7 reductions against {5} generic; a greatest-among aggregate would give 5") {
                    val cost = costOfPricedSpell(game, 1)
                    cost.genericAmount shouldBe 0
                    // CR 601.2f: the mana component floors at {0}; over-reduction never eats a pip.
                    cost.colorCount[Color.RED] shouldBe 1
                }
            }

            test("a bigger creature that isn't a source contributes nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the 4/4 carries no cost modifier, so the reduction is still the 2/3 source's power") {
                    genericCostOfPricedSpell(game, 1) shouldBe 3
                }
            }

            test("power is read from projected state, so an anthem raises the reduction") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Glorious Anthem makes the source a 3/4, so it shaves {3} instead of {2}") {
                    genericCostOfPricedSpell(game, 1) shouldBe 2
                }
            }

            test("a source shrunk below 0 power reduces nothing and never taxes the spell") {
                // CR 107.1b: a calculation that would yield a negative number uses zero instead.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withCardOnBattlefield(1, "Test Withering Field")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the 2/3 source is a -2/3; the reduction floors at 0, it does not add {2}") {
                    val cost = costOfPricedSpell(game, 1)
                    cost.genericAmount shouldBe 5
                    cost.colorCount[Color.RED] shouldBe 1
                }
            }

            test("a shrunk source does not eat another source's discount") {
                // The floor is applied per source (CR 107.1b is per calculation), so the −2 power
                // source contributes 0 while the 5/5 still contributes its full 5.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withCardOnBattlefield(1, "Test Big Discounter")
                    .withCardOnBattlefield(1, "Test Withering Field")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("0 (from the -2/3) + 1 (the 5/5 shrunk to 1/5) = 1 off the {5} generic") {
                    genericCostOfPricedSpell(game, 1) shouldBe 4
                }
            }

            test("the Toughness property reads toughness, not power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Toughness Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("a 0/4 source reduces by 4 on the toughness axis (0 on the power axis)") {
                    genericCostOfPricedSpell(game, 1) shouldBe 1
                }
            }

            test("a CounterCount amount reads the source's own counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Counter Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("no counters yet, so no discount") {
                    genericCostOfPricedSpell(game, 1) shouldBe 5
                }

                val artifact = game.findPermanent("Test Counter Discounter")!!
                game.state = game.state.updateEntity(artifact) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(counters.withAdded(CounterType.CHARGE, 3))
                }

                withClue("three charge counters on the source shave {3} off the {5} generic") {
                    genericCostOfPricedSpell(game, 1) shouldBe 2
                }
            }

            test("an AnyCaster source reads its own power even for an opponent's spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Global Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the amount comes from the permanent carrying the static, not from the caster's board") {
                    genericCostOfPricedSpell(game, 2) shouldBe 2
                }
            }

            test("a YouCast source does not discount an opponent's spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Test Priced Spell")
                    .withCardOnBattlefield(1, "Test Small Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("target matching still gates the modifier; only the source's controller benefits") {
                    genericCostOfPricedSpell(game, 2) shouldBe 5
                }
            }

            test("a SelfCast modifier has no source permanent and reduces nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Self Discounter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Test Self Discounter"),
                    game.player1Id,
                )

                withClue("the card is the spell being cast — there is no permanent to read a power from") {
                    cost.genericAmount shouldBe 6
                    cost.colorCount[Color.RED] shouldBe 1
                }
            }

            test("a SelfCast modifier reads a player-scoped amount relative to the caster") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Self Insight")
                    .withCardInHand(2, "Test Self Insight")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state
                    .updateEntity(game.player2Id) { it.with(LifeLostAmountThisTurnComponent(4)) }
                    .updateEntity(game.player1Id) { it.with(LifeLostAmountThisTurnComponent(1)) }

                val calculator = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator)
                val insight = cardRegistry.requireCard("Test Self Insight")

                withClue("Player1's opponent lost 4, so {6} generic drops to {2}") {
                    calculator.calculateEffectiveCost(game.state, insight, game.player1Id).genericAmount shouldBe 2
                }
                withClue("for Player2 the opponent is Player1, who lost 1") {
                    calculator.calculateEffectiveCost(game.state, insight, game.player2Id).genericAmount shouldBe 5
                }
            }

            test("no source on the battlefield reduces nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Priced Spell")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                genericCostOfPricedSpell(game, 1) shouldBe 5
            }
        }
    }
}
