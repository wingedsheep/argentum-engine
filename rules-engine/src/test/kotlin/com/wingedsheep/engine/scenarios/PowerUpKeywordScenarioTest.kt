package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Feature tests for the **Power-up** keyword (CR 702.193) — "Power-up — [Cost]: [Effect]" means
 * "[Cost]: [Effect]. If this permanent entered this turn, this ability's cost is reduced by this
 * permanent's mana cost. Activate this ability only once."
 *
 * The three clauses, and what pins each one down here:
 *  - **"Activate only once"** — the `isPowerUp` marker desugars to [ActivationRestriction.Once], so
 *    a second activation is illegal for that object's lifetime, and per CR 400.7 a permanent that
 *    leaves and re-enters is a new object that may activate again.
 *  - **The cost reduction** — pip-wise per CR 702.193b/118.7, *not* generic-only. The tests below
 *    deliberately use costs where a generic-only reduction of the same mana value would leave a
 *    cost the player's lands cannot pay, so a regression to `reduceGeneric` fails rather than
 *    silently passing.
 *  - **"If this permanent entered this turn"** — the discount is gone on every later turn, and the
 *    enumerator's displayed cost and the handler's paid cost agree in both states.
 */
class PowerUpKeywordScenarioTest : ScenarioTestBase() {

    /**
     * Kang the Conqueror's shape: `{2}{U}{U}` creature with "Power-up — {5}{U}{U}{U}".
     * Reduced by its own mana cost that is `{3}{U}` — four mana, one blue pip.
     *
     * A *generic-only* reduction of the same size ({4}, the mana value of `{2}{U}{U}`) would give
     * `{1}{U}{U}{U}` instead: also four mana, but three blue pips. The tests fund the player with
     * three Mountains and one Island, which pays `{3}{U}` and cannot pay `{1}{U}{U}{U}` — so the
     * two candidate implementations give opposite answers.
     */
    private val conqueror = card("Temporal Conqueror") {
        manaCost = "{2}{U}{U}"
        typeLine = "Creature — Human Villain"
        power = 2
        toughness = 3
        oracleText = "Power-up — {5}{U}{U}{U}: Put a +1/+1 counter on this creature. " +
            "(Activate each power-up ability only once. Reduce the cost by its mana cost if it entered this turn.)"
        activatedAbility {
            isPowerUp = true
            cost = Costs.Mana("{5}{U}{U}{U}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    /**
     * Thanos's shape, the extreme case: `{R}{W}{B}` reducing `{C}{W}{U}{B}{R}{G}` to `{C}{U}{G}`.
     * Exercises a colorless `{C}` pip surviving on both sides and three colored pips cancelling.
     * Asserted on the displayed cost only — funding six specific pips adds nothing.
     */
    private val madTitan = card("Gauntlet Tyrant") {
        manaCost = "{R}{W}{B}"
        typeLine = "Creature — Eternal Villain"
        power = 4
        toughness = 4
        oracleText = "Power-up — {C}{W}{U}{B}{R}{G}: Put two +1/+1 counters on this creature."
        activatedAbility {
            isPowerUp = true
            cost = Costs.Mana("{C}{W}{U}{B}{R}{G}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        }
    }

    /** Hulk, Gamma Goliath's static: "Power-up abilities of other creatures you control cost {3} less." */
    private val gammaGoliath = card("Gamma Goliath") {
        manaCost = "{3}{R}{G}"
        typeLine = "Legendary Creature — Gamma Hero"
        power = 6
        toughness = 6
        oracleText = "Power-up abilities of other creatures you control cost {3} less to activate."
        staticAbility {
            ability = ReduceActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
                amount = DynamicAmount.Fixed(3),
                powerUpOnly = true
            )
        }
    }

    /** A plain (non power-up) activated ability, to prove `powerUpOnly` does not discount it. */
    private val plainActivator = card("Steady Technician") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Human Scientist"
        power = 1
        toughness = 2
        oracleText = "{5}{U}{U}{U}: Put a +1/+1 counter on this creature."
        activatedAbility {
            cost = Costs.Mana("{5}{U}{U}{U}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    /** A {0} sorcery to bounce a permanent, so it can be replayed as a new object. */
    private val bounce = card("Temporal Recall") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }

    private val conquerorAbilityId
        get() = cardRegistry.getCard("Temporal Conqueror")!!.script.activatedAbilities[0].id
    private val technicianAbilityId
        get() = cardRegistry.getCard("Steady Technician")!!.script.activatedAbilities[0].id

    /**
     * The power-up action the enumerator offers [playerNumber], or null when it offers none. The
     * "Power-up — " prefix is itself part of what's under test: the enumerator rebuilds the label
     * from the *effective* cost, so this both finds the action and reports the cost being asked for.
     */
    private fun TestGame.powerUpAction(playerNumber: Int = 1) =
        getLegalActions(playerNumber).firstOrNull { it.description.startsWith("Power-up —") }

    init {
        cardRegistry.register(conqueror)
        cardRegistry.register(madTitan)
        cardRegistry.register(gammaGoliath)
        cardRegistry.register(plainActivator)
        cardRegistry.register(bounce)

        context("Power-up keyword") {

            test("isPowerUp desugars to ActivationRestriction.Once and prefixes the description") {
                val ability = cardRegistry.getCard("Temporal Conqueror")!!.script.activatedAbilities[0]
                ability.isPowerUp shouldBe true
                withClue("the marker must carry the once-per-object enforcement (CR 702.193a)") {
                    ability.restrictions.contains(ActivationRestriction.Once) shouldBe true
                }
                ability.description shouldStartWith "Power-up — {5}{U}{U}{U}:"
            }

            test("the turn it entered, the cost is reduced pip-wise and is payable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temporal Conqueror", enteredThisTurn = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.powerUpAction()
                withClue("the power-up ability must be offered") { action shouldNotBe null }
                withClue("{5}{U}{U}{U} reduced by {2}{U}{U} is {3}{U}, not the generic-only {1}{U}{U}{U}") {
                    action!!.description shouldStartWith "Power-up — {3}{U}:"
                }
                withClue("three Mountains and an Island pay {3}{U}") {
                    action!!.isAffordable shouldBe true
                }

                val conquerorId = game.findPermanent("Temporal Conqueror")!!
                val result = game.execute(ActivateAbility(game.player1Id, conquerorId, conquerorAbilityId))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("the handler must accept the same reduced cost the enumerator displayed") {
                    game.state.getEntity(conquerorId)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("on a later turn the discount is gone and the printed cost is unaffordable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    // No enteredThisTurn: it has been on the battlefield since an earlier turn.
                    .withCardOnBattlefield(1, "Temporal Conqueror")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.powerUpAction()
                withClue("the printed cost is shown, undiscounted") {
                    action!!.description shouldStartWith "Power-up — {5}{U}{U}{U}:"
                }
                withClue("four lands cannot pay eight mana") { action!!.isAffordable shouldBe false }

                val conquerorId = game.findPermanent("Temporal Conqueror")!!
                val result = game.execute(ActivateAbility(game.player1Id, conquerorId, conquerorAbilityId))
                withClue("the handler must reject what the enumerator called unaffordable") {
                    (result.error != null) shouldBe true
                }
            }

            test("colored and colorless pips cancel pip-wise: {C}{W}{U}{B}{R}{G} less {R}{W}{B} is {C}{U}{G}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gauntlet Tyrant", enteredThisTurn = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("a generic-only reduction would leave the cost untouched — it has no generic mana") {
                    game.powerUpAction()!!.description shouldStartWith "Power-up — {C}{U}{G}:"
                }
            }

            test("activating once exhausts it for that object, even on a later turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temporal Conqueror", enteredThisTurn = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conquerorId = game.findPermanent("Temporal Conqueror")!!
                game.execute(ActivateAbility(game.player1Id, conquerorId, conquerorAbilityId)).error shouldBe null
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()
                game.state.getEntity(conquerorId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

                withClue("an activated power-up must not be offered again") {
                    game.powerUpAction() shouldBe null
                }
                val second = game.execute(ActivateAbility(game.player1Id, conquerorId, conquerorAbilityId))
                withClue("a second activation of the same object must be illegal") {
                    (second.error != null) shouldBe true
                }
                withClue("Once is per object lifetime, not per turn — no second counter") {
                    game.state.getEntity(conquerorId)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("re-entering the battlefield is a new object — power-up may be activated again (CR 400.7)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temporal Conqueror", enteredThisTurn = true)
                    .withCardInHand(1, "Temporal Recall")
                    // {3}{U} to power up, {2}{U}{U} to recast, {3}{U} to power up again.
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val firstObject = game.findPermanent("Temporal Conqueror")!!
                game.execute(ActivateAbility(game.player1Id, firstObject, conquerorAbilityId)).error shouldBe null
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                game.castSpell(1, "Temporal Recall", firstObject).error shouldBe null
                game.resolveStack()
                game.isInHand(1, "Temporal Conqueror") shouldBe true
                game.castSpell(1, "Temporal Conqueror").error shouldBe null
                game.resolveStack()

                val secondObject = game.findPermanent("Temporal Conqueror")!!
                withClue("the recast permanent enters fresh, with no counters") {
                    game.state.getEntity(secondObject)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
                }
                withClue("it entered this turn, so the new object is discounted too") {
                    game.powerUpAction()!!.description shouldStartWith "Power-up — {3}{U}:"
                }

                val reactivate = game.execute(ActivateAbility(game.player1Id, secondObject, conquerorAbilityId))
                withClue("a new object may activate its power-up again: ${reactivate.error}") {
                    reactivate.error shouldBe null
                }
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()
                game.state.getEntity(secondObject)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

            test("a powerUpOnly static stacks with power-up's own reduction (CR 601.2f)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temporal Conqueror", enteredThisTurn = true)
                    .withCardOnBattlefield(1, "Gamma Goliath")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("{5}{U}{U}{U} less {2}{U}{U} is {3}{U}, then {3} less is {U}") {
                    game.powerUpAction()!!.description shouldStartWith "Power-up — {U}:"
                }
            }

            test("a powerUpOnly static leaves an ordinary activated ability at full price") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Steady Technician")
                    .withCardOnBattlefield(1, "Gamma Goliath")
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val action = game.getLegalActions(1).firstOrNull {
                    it.action.let { a -> a is ActivateAbility && a.abilityId == technicianAbilityId }
                }
                withClue("the technician's ability must be offered") { action shouldNotBe null }
                withClue("powerUpOnly gates on the ability, so a plain one keeps its printed cost") {
                    action!!.description shouldStartWith "{5}{U}{U}{U}:"
                }
            }
        }
    }
}
