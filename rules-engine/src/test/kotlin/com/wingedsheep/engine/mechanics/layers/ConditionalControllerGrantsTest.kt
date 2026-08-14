package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.mechanics.targeting.ControllerHexproof
import com.wingedsheep.engine.mechanics.targeting.ControllerShroud
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.ControllerGrantMarker
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameFromLifeComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerProtectionComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsOpponentsCantWinGameComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsSacrificeImmunityComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsStationUsingToughnessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeTargetedByOpponentAbilities
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.GrantShroudToController
import com.wingedsheep.sdk.scripting.OpponentsCantMakeYouSacrifice
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.StationUsingToughness
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Every [ControllerGrantMarker] must survive being wrapped in a [ConditionalStaticAbility].
 *
 * These grants sit outside the Rule 613 layer system: `StaticAbilityHandler` stamps a marker
 * component **once**, as the permanent enters, and readers later scan the battlefield for it.
 * Because the stamp happens once, a gate can't be resolved at stamp time — it has to travel on the
 * marker and be re-evaluated on every read.
 *
 * Two ways to get that wrong, and this file exists to fail the build on both:
 *
 *  1. **Stamping with a bare `any { it is A }`.** A wrapped ability is a `ConditionalStaticAbility`,
 *     not an `A`, so the marker is never stamped and the ability does *nothing at all* — not
 *     "always on", not "always off", simply absent. No compile error, no warning, and the obvious
 *     half of a hand-written scenario test ("condition false → no grant") passes trivially. This is
 *     the silent no-op that shipped for hexproof until Captain America, Super-Soldier hit it.
 *  2. **Reading with a bare `container.has<M>()`.** The marker is stamped but the gate is ignored,
 *     so the grant is stuck permanently on.
 *
 * `grantCases` is the roster. When you add a `ControllerGrantMarker`, add it here too — the
 * roster-completeness test at the bottom fails if you don't.
 */
class ConditionalControllerGrantsTest : FunSpec({

    val handler = StaticAbilityHandler(CardRegistry())
    val player = EntityId.generate()

    /** Cheap, easy-to-flip gate: "as long as this permanent has a shield counter on it". */
    val gate: Condition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.SHIELD))

    /**
     * One marker under test: the SDK ability that produces it, and how to read the stamped
     * component's gate back off a container ([conditionOf] returns `null` both when the marker is
     * absent and when it carries no gate — [stamped] separates those two cases).
     */
    data class GrantCase(
        val label: String,
        val ability: StaticAbility,
        val marker: Class<out ControllerGrantMarker>,
        val stamped: (ComponentContainer) -> Boolean,
        val conditionOf: (ComponentContainer) -> Condition?,
    )

    val grantCases = listOf(
        GrantCase(
            "GrantShroudToController", GrantShroudToController,
            GrantsControllerShroudComponent::class.java,
            { it.get<GrantsControllerShroudComponent>() != null },
            { it.get<GrantsControllerShroudComponent>()?.condition },
        ),
        GrantCase(
            "GrantHexproofToController", GrantHexproofToController,
            GrantsControllerHexproofComponent::class.java,
            { it.get<GrantsControllerHexproofComponent>() != null },
            { it.get<GrantsControllerHexproofComponent>()?.condition },
        ),
        GrantCase(
            "OpponentsCantMakeYouSacrifice", OpponentsCantMakeYouSacrifice,
            GrantsSacrificeImmunityComponent::class.java,
            { it.get<GrantsSacrificeImmunityComponent>() != null },
            { it.get<GrantsSacrificeImmunityComponent>()?.condition },
        ),
        GrantCase(
            "GrantCantLoseGame", GrantCantLoseGame,
            GrantsCantLoseGameComponent::class.java,
            { it.get<GrantsCantLoseGameComponent>() != null },
            { it.get<GrantsCantLoseGameComponent>()?.condition },
        ),
        GrantCase(
            "GrantOpponentsCantWinGame", GrantOpponentsCantWinGame,
            GrantsOpponentsCantWinGameComponent::class.java,
            { it.get<GrantsOpponentsCantWinGameComponent>() != null },
            { it.get<GrantsOpponentsCantWinGameComponent>()?.condition },
        ),
        GrantCase(
            "GrantCantLoseGameFromLife", GrantCantLoseGameFromLife,
            GrantsCantLoseGameFromLifeComponent::class.java,
            { it.get<GrantsCantLoseGameFromLifeComponent>() != null },
            { it.get<GrantsCantLoseGameFromLifeComponent>()?.condition },
        ),
        GrantCase(
            "StationUsingToughness", StationUsingToughness,
            GrantsStationUsingToughnessComponent::class.java,
            { it.get<GrantsStationUsingToughnessComponent>() != null },
            { it.get<GrantsStationUsingToughnessComponent>()?.condition },
        ),
        GrantCase(
            "CantBeTargetedByOpponentAbilities", CantBeTargetedByOpponentAbilities,
            CantBeTargetedByOpponentAbilitiesComponent::class.java,
            { it.get<CantBeTargetedByOpponentAbilitiesComponent>() != null },
            { it.get<CantBeTargetedByOpponentAbilitiesComponent>()?.condition },
        ),
    )

    fun cardWith(vararg statics: StaticAbility) = CardDefinition(
        name = "Test Grant Source",
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
        creatureStats = CreatureStats(1, 1),
        script = CardScript(staticAbilities = statics.toList()),
    )

    fun stamp(vararg statics: StaticAbility): ComponentContainer =
        handler.addContinuousEffectComponent(ComponentContainer(), cardWith(*statics))

    // =========================================================================
    // Stamping — the silent no-op
    // =========================================================================

    context("a bare grant stamps its marker with no gate") {
        grantCases.forEach { case ->
            test(case.label) {
                val container = stamp(case.ability)
                withClue("marker must be stamped for the bare form") {
                    case.stamped(container) shouldBe true
                }
                withClue("a bare grant is unconditional, so it carries no condition") {
                    case.conditionOf(container) shouldBe null
                }
            }
        }
    }

    context("a gated grant stamps its marker AND carries the gate") {
        grantCases.forEach { case ->
            test(case.label) {
                val container = stamp(ConditionalStaticAbility(ability = case.ability, condition = gate))
                withClue(
                    "${case.label} wrapped in ConditionalStaticAbility stamped no marker — the " +
                        "ability is silently inert. StaticAbilityHandler is matching the bare " +
                        "type instead of going through controllerGrant<A>()."
                ) {
                    case.stamped(container) shouldBe true
                }
                withClue("the gate must travel on the marker so readers can re-evaluate it") {
                    case.conditionOf(container) shouldBe gate
                }
            }
        }
    }

    test("player-level protection gates per scope, not per permanent") {
        // One permanent, two GrantProtectionToController abilities, only one of them gated —
        // the reason this marker holds a list of ProtectionGrant rather than one condition.
        val container = stamp(
            GrantProtectionToController(ProtectionScope.EachOpponent),
            ConditionalStaticAbility(
                ability = GrantProtectionToController(ProtectionScope.Everything),
                condition = gate,
            ),
        )
        val grants = container.get<GrantsControllerProtectionComponent>()?.grants
        grants.shouldNotBeNull()
        withClue("both abilities must be stamped, gated or not") { grants.size shouldBe 2 }
        grants.single { it.scope == ProtectionScope.EachOpponent }.condition shouldBe null
        grants.single { it.scope == ProtectionScope.Everything }.condition shouldBe gate
    }

    // =========================================================================
    // Reading — the gate has to actually flip
    // =========================================================================

    /** A battlefield holding one permanent controlled by [player], carrying [components]. */
    fun battlefieldWith(
        permanentId: EntityId,
        container: ComponentContainer,
        shieldCounters: Int,
    ): GameState {
        var withCounters = container
            .with(
                CardComponent(
                    cardDefinitionId = "Test Grant Source",
                    name = "Test Grant Source",
                    manaCost = ManaCost(emptyList()),
                    typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                    ownerId = player,
                    baseStats = CreatureStats(1, 1),
                )
            )
            .with(OwnerComponent(player))
            .with(ControllerComponent(player))
        if (shieldCounters > 0) {
            withCounters = withCounters.with(
                CountersComponent(mapOf(CounterType.SHIELD to shieldCounters))
            )
        }
        return GameState()
            .withEntity(player, ComponentContainer())
            .withEntity(permanentId, withCounters)
            .addToZone(ZoneKey(player, Zone.BATTLEFIELD), permanentId)
    }

    test("ControllerGrants.isActive is false without the counter and true with it") {
        val permanentId = EntityId.generate()
        val stamped = stamp(ConditionalStaticAbility(GrantShroudToController, gate))

        withClue("gate open: no shield counter, so the grant is off") {
            val state = battlefieldWith(permanentId, stamped, shieldCounters = 0)
            ControllerGrants.isActive(state, permanentId, gate) shouldBe false
        }
        withClue("gate closed: a shield counter switches the grant on") {
            val state = battlefieldWith(permanentId, stamped, shieldCounters = 1)
            ControllerGrants.isActive(state, permanentId, gate) shouldBe true
        }
    }

    test("an unconditional grant is always active") {
        val permanentId = EntityId.generate()
        val state = battlefieldWith(permanentId, stamp(GrantShroudToController), shieldCounters = 0)
        ControllerGrants.isActive(state, permanentId, condition = null) shouldBe true
    }

    context("the facade readers honour the gate") {
        test("ControllerShroud") {
            val permanentId = EntityId.generate()
            val stamped = stamp(ConditionalStaticAbility(GrantShroudToController, gate))
            ControllerShroud.appliesTo(
                battlefieldWith(permanentId, stamped, shieldCounters = 0), player
            ) shouldBe false
            ControllerShroud.appliesTo(
                battlefieldWith(permanentId, stamped, shieldCounters = 1), player
            ) shouldBe true
        }

        test("ControllerHexproof") {
            val permanentId = EntityId.generate()
            val stamped = stamp(ConditionalStaticAbility(GrantHexproofToController, gate))
            ControllerHexproof.appliesTo(
                battlefieldWith(permanentId, stamped, shieldCounters = 0), player
            ) shouldBe false
            ControllerHexproof.appliesTo(
                battlefieldWith(permanentId, stamped, shieldCounters = 1), player
            ) shouldBe true
        }

        test("an ungated grant still reads as on") {
            val permanentId = EntityId.generate()
            val stamped = stamp(GrantShroudToController)
            ControllerShroud.appliesTo(
                battlefieldWith(permanentId, stamped, shieldCounters = 0), player
            ) shouldBe true
        }
    }

    // =========================================================================
    // Roster completeness
    // =========================================================================

    test("every ControllerGrantMarker is covered by this file") {
        // Reflection would need classpath scanning the engine doesn't otherwise do, so this is a
        // hand-maintained list checked against the roster above. Adding a marker without adding a
        // GrantCase leaves it untested — and the bug this file guards is invisible without a test.
        val knownMarkers = setOf<Class<out ControllerGrantMarker>>(
            GrantsControllerShroudComponent::class.java,
            GrantsControllerHexproofComponent::class.java,
            GrantsSacrificeImmunityComponent::class.java,
            GrantsCantLoseGameComponent::class.java,
            GrantsOpponentsCantWinGameComponent::class.java,
            GrantsCantLoseGameFromLifeComponent::class.java,
            GrantsStationUsingToughnessComponent::class.java,
            CantBeTargetedByOpponentAbilitiesComponent::class.java,
        )
        withClue("grantCases must cover every marker in knownMarkers") {
            grantCases.map { it.marker }.toSet() shouldBe knownMarkers
        }
    }
})
