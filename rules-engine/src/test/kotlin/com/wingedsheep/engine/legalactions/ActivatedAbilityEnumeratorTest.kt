package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainActivatedAbilityOn
import com.wingedsheep.engine.legalactions.support.shouldNotContainActivatedAbilityOn
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.OrcishVandal
import com.wingedsheep.mtg.sets.definitions.khans.cards.ArchersParapet
import com.wingedsheep.mtg.sets.definitions.khans.cards.DisownedAncestor
import com.wingedsheep.mtg.sets.definitions.khans.cards.EmbodimentOfSpring
import com.wingedsheep.mtg.sets.definitions.khans.cards.PearlLakeAncient
import com.wingedsheep.mtg.sets.definitions.khans.cards.RakshasaDeathdealer
import com.wingedsheep.mtg.sets.definitions.khans.cards.RetributionOfTheAncients
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.ActivatedAbilityEnumerator].
 *
 * Paths covered:
 * - Mana-cost-only activated abilities (Rakshasa Deathdealer's +2/+2 and regenerate)
 * - Composite Tap+Mana costs (Archers' Parapet): tapped source, summoning sickness
 *   — both yield unaffordable entries (not dropped) because cost-pay-check sets
 *   costAffordable=false rather than `continue`-skipping inside the Composite branch
 * - Pure Tap cost drops unaffordable entries entirely (Visara the Dreadful): the
 *   top-level `AbilityCost.Tap` branch `continue`s — unlike the Composite branch
 * - Sorcery-speed restriction (Disowned Ancestor's outlast at upkeep)
 * - Activation restrictions (Weathered Wayfarer's "only if an opponent controls
 *   more lands")
 * - Unaffordable cost emits greyed entry
 * - Face-down suppression (Rule 707.2)
 * - Opponent's permanents not surfaced for me
 * - Target-requirement paths — ordinary `TargetCreature` (Visara the Dreadful)
 * - Composite Tap + Sacrifice (Orcish Vandal): `SacrificePermanent` cost info
 * - Composite Mana + Tap + SacrificeSelf (Embodiment of Spring)
 * - TapPermanents cost with target (Catapult Master): `TapPermanents` cost info
 * - ReturnToHand cost (Pearl Lake Ancient): `BouncePermanent` cost info
 * - X-variable RemoveXPlusOnePlusOneCounters (Retribution of the Ancients)
 * - AnyPlayerMay path — opponent's Lethal Vapors surfaces a Free-cost activation
 *
 * Deferred to a follow-up phase: granted-ability grants from static effects,
 * planeswalker loyalty limits, class level-up, tap-attached-creature
 * (Equipment-style), convoke on activated abilities, self-target auto-select,
 * auto-select-player path.
 */
class ActivatedAbilityEnumeratorTest : FunSpec({

    /** Battlefield entity id for the P1 permanent matching [name]. */
    fun entityOnBattlefield(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getBattlefield(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    // -------------------------------------------------------------------------
    context("Mana-cost-only activated abilities (own permanent)") {

        test("Rakshasa Deathdealer in play with {B}{G} available surfaces BOTH activated abilities") {
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            val view = driver.enumerateFor(driver.player1)

            view shouldContainActivatedAbilityOn rakshasaId
            // Two activated abilities: +2/+2 and regenerate. Both pure {B}{G}.
            val abilities = view.activatedAbilityActionsFor(rakshasaId)
            abilities shouldHaveSize 2
            abilities.forEach { it.affordable shouldBe true }
            abilities.forEach { it.manaCostString shouldBe "{B}{G}" }
        }

        test("without the right mana, the ability is still emitted but marked unaffordable") {
            // Only a Forest — can't pay {B}.
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(rakshasaId)

            abilities shouldHaveSize 2
            abilities.forEach { it.affordable shouldBe false }
        }

        test("no source on battlefield produces no activated-ability actions for that card") {
            // Put only lands down; Rakshasa lives in the graveyard — not a battlefield permanent.
            val driver = setupP1(
                battlefield = listOf("Swamp", "Forest"),
                graveyard = listOf("Rakshasa Deathdealer"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )

            driver.enumerateFor(driver.player1)
                .filter {
                    (it.action as? ActivateAbility)
                        ?.let { act -> driver.game.state.getEntity(act.sourceId)?.get<CardComponent>()?.name == "Rakshasa Deathdealer" }
                        ?: false
                }.shouldBeEmpty()
        }

        test("opponent controls Rakshasa — I do NOT see its activated abilities in my list") {
            // P1 has lands but no Rakshasa; Rakshasa lives on P1's battlefield — we enumerate as P2.
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")

            driver.enumerateFor(driver.player2) shouldNotContainActivatedAbilityOn rakshasaId
        }
    }

    // -------------------------------------------------------------------------
    context("Composite Tap+Mana cost (Archers' Parapet)") {

        test("untapped source with mana and no summoning sickness surfaces the ability") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe true
        }

        test("source already tapped — ability emitted as unaffordable (Composite path)") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")
            // Tap the Parapet.
            val tapped = driver.game.state.getEntity(parapetId)!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(parapetId, tapped))

            // Composite cost path: Tap sub-cost fails → costCanBePaid=false →
            // the ability still emits as a greyed-out (affordable=false) entry.
            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe false
        }

        test("source with summoning sickness and no haste — ability emitted as unaffordable") {
            val driver = setupP1(
                battlefield = listOf("Archers' Parapet", "Swamp", "Swamp"),
                extraSetCards = listOf(ArchersParapet)
            )
            val parapetId = entityOnBattlefield(driver, "Archers' Parapet")
            // Simulate a just-played creature with summoning sickness.
            val sick = driver.game.state.getEntity(parapetId)!!.with(SummoningSicknessComponent)
            driver.game.replaceState(driver.game.state.withEntity(parapetId, sick))

            val abilities = driver.enumerateFor(driver.player1).activatedAbilityActionsFor(parapetId)

            abilities shouldHaveSize 1
            abilities.single().affordable shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    context("Sorcery-speed timing (Disowned Ancestor's Outlast)") {

        test("Outlast is enumerated on the active player's main phase") {
            val driver = setupP1(
                battlefield = listOf("Disowned Ancestor", "Swamp", "Forest"),
                extraSetCards = listOf(DisownedAncestor)
            )
            val ancestorId = entityOnBattlefield(driver, "Disowned Ancestor")

            driver.enumerateFor(driver.player1) shouldContainActivatedAbilityOn ancestorId
        }

        test("Outlast is NOT enumerated on the upkeep step (sorcery-speed only)") {
            val driver = setupP1(
                battlefield = listOf("Disowned Ancestor", "Swamp", "Forest"),
                extraSetCards = listOf(DisownedAncestor),
                atStep = Step.UPKEEP
            )
            val ancestorId = entityOnBattlefield(driver, "Disowned Ancestor")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn ancestorId
        }
    }

    // -------------------------------------------------------------------------
    context("Activation restrictions (Weathered Wayfarer's OnlyIfCondition)") {

        test("opponent controls no more lands than me — restriction blocks the ability") {
            // P1 battlefield: Wayfarer + 1 Plains (1 land). P2 runs a pure-Forest
            // deck, so P2 has 0 lands on battlefield → opponent does NOT control
            // more lands. Restriction not met → ability is NOT enumerated.
            val driver = setupP1(
                battlefield = listOf("Weathered Wayfarer", "Plains")
            )
            val wayfarerId = entityOnBattlefield(driver, "Weathered Wayfarer")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn wayfarerId
        }

        test("opponent controls more lands than me — restriction met, ability surfaces") {
            // Bring P2 up to 2 Forests on battlefield — more than my 0 lands.
            val driver = setupP1(
                battlefield = listOf("Weathered Wayfarer")
            )
            val wayfarerId = entityOnBattlefield(driver, "Weathered Wayfarer")

            // Surgically move 2 Forests from P2's library to P2's battlefield.
            var state = driver.game.state
            val p2LibKey = com.wingedsheep.engine.state.ZoneKey(driver.player2, com.wingedsheep.sdk.core.Zone.LIBRARY)
            val p2BattlefieldKey = com.wingedsheep.engine.state.ZoneKey(driver.player2, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
            repeat(2) {
                val forestId = state.getZone(p2LibKey).first { id ->
                    state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                }
                state = state.moveToZone(forestId, p2LibKey, p2BattlefieldKey)
            }
            driver.game.replaceState(state)

            driver.enumerateFor(driver.player1) shouldContainActivatedAbilityOn wayfarerId
        }
    }

    // -------------------------------------------------------------------------
    context("Face-down suppression (Rule 707.2)") {

        test("a face-down permanent produces no activated abilities") {
            val driver = setupP1(
                battlefield = listOf("Rakshasa Deathdealer", "Swamp", "Forest"),
                extraSetCards = listOf(RakshasaDeathdealer)
            )
            val rakshasaId = entityOnBattlefield(driver, "Rakshasa Deathdealer")
            // Flip Rakshasa face-down (simulating morph).
            val hidden = driver.game.state.getEntity(rakshasaId)!!.with(FaceDownComponent)
            driver.game.replaceState(driver.game.state.withEntity(rakshasaId, hidden))

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn rakshasaId
        }
    }

    // -------------------------------------------------------------------------
    context("Target requirements — ordinary TargetCreature (Visara the Dreadful)") {

        test("untapped Visara with a target creature — ability emitted with validTargets") {
            val driver = setupP1(
                battlefield = listOf("Visara the Dreadful", "Grizzly Bears")
            )
            val visaraId = entityOnBattlefield(driver, "Visara the Dreadful")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(visaraId).single()

            ability.requiresTargets shouldBe true
            ability.targetCount shouldBe 1
            val targets = ability.validTargets.shouldNotBeNull()
            // validTargets is the set of creatures — Visara plus Grizzly Bears.
            targets shouldHaveSize 2
        }

        test("tapped Visara — pure Tap cost drops the ability entirely (NOT as unaffordable entry)") {
            // This distinguishes the top-level `AbilityCost.Tap` branch (which
            // `continue`s) from the Composite branch (which emits affordable=false).
            val driver = setupP1(
                battlefield = listOf("Visara the Dreadful", "Grizzly Bears")
            )
            val visaraId = entityOnBattlefield(driver, "Visara the Dreadful")
            val tapped = driver.game.state.getEntity(visaraId)!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(visaraId, tapped))

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn visaraId
        }
    }

    // -------------------------------------------------------------------------
    context("Composite Tap + Sacrifice artifact (Orcish Vandal)") {

        test("with an artifact to sacrifice — ability emits SacrificePermanent cost info") {
            val driver = setupP1(
                battlefield = listOf("Orcish Vandal", "Artifact Creature"),
                extraSetCards = listOf(OrcishVandal)
            )
            val vandalId = entityOnBattlefield(driver, "Orcish Vandal")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(vandalId).single()

            ability.affordable shouldBe true
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "SacrificePermanent"
            costInfo.sacrificeCount shouldBe 1
            // Exactly one artifact to sacrifice (the Artifact Creature).
            costInfo.validSacrificeTargets shouldHaveSize 1
        }

        test("without an artifact — ability emitted as unaffordable (Composite path, Sacrifice sub-cost unpayable)") {
            // The Composite branch sets `costAffordable=false` rather than `continue`,
            // so an ability with one unpayable sub-cost still emits as greyed-out.
            val driver = setupP1(
                battlefield = listOf("Orcish Vandal", "Grizzly Bears"),  // no artifacts
                extraSetCards = listOf(OrcishVandal)
            )
            val vandalId = entityOnBattlefield(driver, "Orcish Vandal")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(vandalId).single()

            ability.affordable shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    context("Composite Mana + Tap + SacrificeSelf (Embodiment of Spring)") {

        test("with mana and untapped source — ability emits and is affordable") {
            val driver = setupP1(
                battlefield = listOf("Embodiment of Spring", "Forest", "Forest"),
                extraSetCards = listOf(EmbodimentOfSpring)
            )
            val embId = entityOnBattlefield(driver, "Embodiment of Spring")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(embId).single()

            ability.affordable shouldBe true
            ability.manaCostString shouldBe "{1}{G}"
        }
    }

    // -------------------------------------------------------------------------
    context("TapPermanents cost with target (Catapult Master)") {

        test("with 5 untapped Soldiers and a target creature — ability emitted with TapPermanents info") {
            // Catapult Master is itself a Soldier. 4 Test Clerics won't match; need
            // 4 more Soldiers — use 4 more Catapult Masters? Can't have 5 legendaries...
            // The filter is Soldier, not just creature. Catapult Master (1) +
            // 4 other Soldier-subtype creatures. No registered test Soldier, so we
            // use 5 Catapult Masters (legendary rule is a state-based action, not
            // enforced at action enumeration — 5 on the battlefield is legal here).
            val driver = setupP1(
                battlefield = listOf(
                    "Catapult Master", "Catapult Master", "Catapult Master",
                    "Catapult Master", "Catapult Master",
                    "Grizzly Bears"  // target candidate
                )
            )
            val masterId = entityOnBattlefield(driver, "Catapult Master")

            // All 5 Catapult Master copies share the same activated ability, so
            // every one emits its own entry. Pick one and check cost info shape.
            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(masterId).single()

            ability.requiresTargets shouldBe true
            ability.targetCount shouldBe 1
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "TapPermanents"
            costInfo.tapCount shouldBe 5
            // 5 Soldiers match the filter for tap targets.
            costInfo.validTapTargets shouldHaveSize 5
        }

        test("with only 4 untapped Soldiers — ability NOT emitted (need 5)") {
            val driver = setupP1(
                battlefield = listOf(
                    "Catapult Master", "Catapult Master", "Catapult Master",
                    "Catapult Master",  // only 4 Soldiers
                    "Grizzly Bears"
                )
            )
            val masterId = entityOnBattlefield(driver, "Catapult Master")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn masterId
        }
    }

    // -------------------------------------------------------------------------
    context("ReturnToHand cost (Pearl Lake Ancient)") {

        test("with 3 lands controlled — ability emits with BouncePermanent cost info") {
            val driver = setupP1(
                battlefield = listOf("Pearl Lake Ancient", "Island", "Island", "Island"),
                extraSetCards = listOf(PearlLakeAncient)
            )
            val leviathanId = entityOnBattlefield(driver, "Pearl Lake Ancient")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(leviathanId).single()

            ability.affordable shouldBe true
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "BouncePermanent"
            costInfo.bounceCount shouldBe 3
            costInfo.validBounceTargets shouldHaveSize 3
        }

        test("with only 2 lands — ability NOT emitted (need 3 bounce targets)") {
            val driver = setupP1(
                battlefield = listOf("Pearl Lake Ancient", "Island", "Island"),
                extraSetCards = listOf(PearlLakeAncient)
            )
            val leviathanId = entityOnBattlefield(driver, "Pearl Lake Ancient")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn leviathanId
        }
    }

    // -------------------------------------------------------------------------
    context("X-variable RemoveXPlusOnePlusOneCounters (Retribution of the Ancients)") {

        test("with {B} available, a +1/+1-counter creature, and a target — ability emits with X cost info") {
            val driver = setupP1(
                battlefield = listOf("Retribution of the Ancients", "Swamp", "Grizzly Bears"),
                extraSetCards = listOf(RetributionOfTheAncients)
            )
            val retributionId = entityOnBattlefield(driver, "Retribution of the Ancients")
            // Put 2 +1/+1 counters on the Grizzly Bears.
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")
            val boosted = driver.game.state.getEntity(bearsId)!!.let { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }
            driver.game.replaceState(driver.game.state.withEntity(bearsId, boosted))

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(retributionId).single()

            ability.hasXCost shouldBe true
            ability.maxAffordableX shouldBe 2  // 2 counters available
            ability.requiresTargets shouldBe true
            ability.manaCostString shouldBe "{B}"
            // The cost info carries the counter-removal creature list.
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.counterRemovalCreatures shouldHaveSize 1
            costInfo.counterRemovalCreatures.single().availableCounters shouldBe 2
        }
    }

    // -------------------------------------------------------------------------
    context("AnyPlayerMay (Lethal Vapors)") {

        test("opponent controls Lethal Vapors — I can activate it via any-player path (Free cost)") {
            // P1 controls Lethal Vapors; we enumerate as P2 so the any-player path
            // fires against P1's battlefield (opponent from P2's perspective).
            val driver = setupP1(
                battlefield = listOf("Lethal Vapors")
            )
            val vaporsId = entityOnBattlefield(driver, "Lethal Vapors")

            val view = driver.enumerateFor(driver.player2)

            view shouldContainActivatedAbilityOn vaporsId
            val ability = view.activatedAbilityActionsFor(vaporsId).single()
            // Free cost → no mana string.
            ability.manaCostString shouldBe null
            // The action is attributed to the ACTIVATING player (P2), not the controller.
            val action = ability.action as ActivateAbility
            action.playerId shouldBe driver.player2
            action.sourceId shouldBe vaporsId
        }

        test("controller also sees the ability through the own-permanents path") {
            // AnyPlayerMay does not block the controller — they can activate it too.
            val driver = setupP1(
                battlefield = listOf("Lethal Vapors")
            )
            val vaporsId = entityOnBattlefield(driver, "Lethal Vapors")

            driver.enumerateFor(driver.player1) shouldContainActivatedAbilityOn vaporsId
        }
    }

})
