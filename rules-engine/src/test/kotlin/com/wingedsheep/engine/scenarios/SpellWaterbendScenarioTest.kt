package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the spell-level **waterbend** additional cost (Avatar: The Last Airbender):
 * *"As an additional cost to cast this spell, [you may] waterbend {N}."*
 *
 * Waterbend {N} adds {N} generic mana to the spell's cost; while paying it the caster may tap
 * untapped artifacts/creatures they control, each paying {1} (generic-only). Modeled by
 * [com.wingedsheep.sdk.scripting.SpellWaterbendCost] on the card script + the existing
 * [AlternativePaymentChoice.tapForGenericPermanents] carrier.
 *
 * Cards (defined inline — no fixed-amount spell-waterbend TLA card is in the test pool):
 *  - "Mandatory Tide" — {1}{U} Sorcery, "waterbend {3}. You gain 5 life."
 *  - "Optional Tide"  — {U} Sorcery, "you may waterbend {4}. You gain 3 life; if paid, 10 instead."
 *
 * Rules pinned:
 *  1. A mandatory waterbend cost is always part of the cost; it can be paid entirely with mana.
 *  2. The waterbend portion can be paid by tapping artifacts/creatures (each {1}).
 *  3. Waterbend taps pay ONLY the waterbend {N}, never the spell's own generic (cap at N).
 *  4. An optional "you may waterbend" is offered as two cast actions (paid / not paid); the paid
 *     path sets the WaterbendWasPaid flag so the effect branches.
 *  5. The cast legal action surfaces hasTapForGeneric + the eligible permanents.
 */
class SpellWaterbendScenarioTest : ScenarioTestBase() {

    init {
        val mandatory = card("Mandatory Tide") {
            manaCost = "{1}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "As an additional cost to cast this spell, waterbend {3}.\nYou gain 5 life."
            waterbendCost(amount = 3)
            spell {
                effect = Effects.GainLife(5)
            }
        }
        cardRegistry.register(mandatory)

        val optional = card("Optional Tide") {
            manaCost = "{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "As an additional cost to cast this spell, you may waterbend {4}.\n" +
                "You gain 3 life. If this spell's additional cost was paid, you gain 10 life instead."
            waterbendCost(amount = 4, optional = true)
            spell {
                effect = ConditionalEffect(
                    condition = Conditions.WaterbendWasPaid,
                    effect = Effects.GainLife(10),
                    elseEffect = Effects.GainLife(3)
                )
            }
        }
        cardRegistry.register(optional)

        // "waterbend {X}" shape — the spell carries no printed {X}; the waterbend cost is what makes
        // it X-carrying. X feeds the effect via DynamicAmount.XValue.
        val variable = card("Tidal Surge X") {
            manaCost = "{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "As an additional cost to cast this spell, waterbend {X}. You gain X life."
            waterbendCost(isX = true)
            spell {
                effect = Effects.GainLife(DynamicAmount.XValue)
            }
        }
        cardRegistry.register(variable)

        fun castAction(game: TestGame, predicate: (LegalActionInfo) -> Boolean) =
            game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && predicate(it)
            }

        context("Mandatory spell waterbend cost") {

            test("can be cast paying the whole waterbend cost with mana") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mandatory Tide")
                    .withLandsOnBattlefield(1, "Island", 5) // {4}{U} = base {1}{U} + waterbend {3}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val action = castAction(game) { it.isAffordable }
                withClue("the mandatory-waterbend spell should be castable for {4}{U}") {
                    action shouldNotBe null
                    action!!.hasTapForGeneric shouldBe true
                }

                val result = game.execute(action!!.action)
                withClue("casting with full mana should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe before + 5
            }

            test("waterbend portion can be paid by tapping creatures") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mandatory Tide")
                    .withLandsOnBattlefield(1, "Island", 2)   // pays the base {1}{U}
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker") // 3 creatures pay waterbend {3}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val creatures = game.findAllPermanents("Glory Seeker")
                creatures.size shouldBe 3
                val action = castAction(game) { it.isAffordable }
                action shouldNotBe null

                val cast = (action!!.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                )
                val result = game.execute(cast)
                withClue("paying the waterbend {3} by tapping 3 creatures should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe before + 5
                withClue("the 3 tapped creatures should be tapped") {
                    creatures.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("waterbend taps cannot pay the spell's own generic (capped at N)") {
                // Base {1}{U} + waterbend {3} = {4}{U}. With only one Island (one {U}) and four
                // creatures, the taps can cover at most the waterbend {3}, leaving the base {1}
                // generic with no mana to pay it — so the spell is not castable.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mandatory Tide")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val affordable = castAction(game) { it.isAffordable }
                withClue("taps cap at the waterbend {3}; the base {1} still needs mana, so it's unaffordable") {
                    affordable shouldBe null
                }
            }
        }

        context("Optional spell waterbend cost") {

            test("declining the optional waterbend uses the base effect") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Optional Tide")
                    .withLandsOnBattlefield(1, "Island", 1) // only the base {U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val unpaid = castAction(game) { it.isAffordable && !it.hasTapForGeneric }
                withClue("an unpaid cast action should be offered") { unpaid shouldNotBe null }

                val result = game.execute(unpaid!!.action)
                withClue("casting without the optional waterbend should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                withClue("base effect: gain 3 life") { game.getLifeTotal(1) shouldBe before + 3 }
            }

            test("paying the optional waterbend uses the enhanced effect") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Optional Tide")
                    .withLandsOnBattlefield(1, "Island", 1) // base {U}
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker") // 4 creatures pay waterbend {4}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val creatures = game.findAllPermanents("Glory Seeker")
                creatures.size shouldBe 4
                val paid = castAction(game) { it.isAffordable && it.hasTapForGeneric }
                withClue("a paid (waterbend) cast action should be offered") {
                    paid shouldNotBe null
                    (paid!!.action as CastSpell).wasWaterbendPaid shouldBe true
                }

                val cast = (paid!!.action as CastSpell).copy(
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                )
                val result = game.execute(cast)
                withClue("paying the optional waterbend by tapping 4 creatures should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                withClue("enhanced effect: gain 10 life") { game.getLifeTotal(1) shouldBe before + 10 }
            }

            test("an optional waterbend spell offers both a paid and an unpaid cast action") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Optional Tide")
                    .withLandsOnBattlefield(1, "Island", 5) // can afford both base {U} and {4}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val casts = game.getLegalActions(1).filter {
                    it.actionType == "CastSpell" && it.action is CastSpell && it.isAffordable
                }
                withClue("both an unpaid and a paid cast variant should be offered") {
                    casts.any { !it.hasTapForGeneric } shouldBe true
                    casts.any { it.hasTapForGeneric } shouldBe true
                }
            }
        }

        context("Variable waterbend {X} cost") {

            test("offered as an X-carrying waterbend cast with X bounded by mana plus tappable permanents") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Tidal Surge X")
                    .withLandsOnBattlefield(1, "Island", 1)   // base {U}, no mana left for X
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker") // 3 tappable -> X can be up to 3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = castAction(game) { it.isAffordable && it.hasTapForGeneric }
                withClue("the waterbend {X} spell should be offered as an X-carrying waterbend cast") {
                    action shouldNotBe null
                    action!!.hasXCost shouldBe true
                    // 1 Island pays the base {U}; the 3 tappable permanents each pay {1} of the X.
                    action.maxAffordableX shouldBe 3
                }
            }

            test("X is paid by tapping permanents and feeds the effect") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Tidal Surge X")
                    .withLandsOnBattlefield(1, "Island", 1)   // base {U}
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker") // tap all 3 to pay waterbend {3}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val creatures = game.findAllPermanents("Glory Seeker")
                creatures.size shouldBe 3
                val action = castAction(game) { it.isAffordable && it.hasTapForGeneric }
                action shouldNotBe null

                // Player chooses X = 3 and taps the 3 creatures to pay the waterbend {3}; {U} from the Island.
                val cast = (action!!.action as CastSpell).copy(
                    xValue = 3,
                    alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = creatures.toSet())
                )
                val result = game.execute(cast)
                withClue("casting waterbend {X=3} by tapping 3 creatures should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                withClue("effect uses X: gain 3 life") { game.getLifeTotal(1) shouldBe before + 3 }
                withClue("all 3 creatures tapped to pay the waterbend {3}") {
                    creatures.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }
        }
    }
}
