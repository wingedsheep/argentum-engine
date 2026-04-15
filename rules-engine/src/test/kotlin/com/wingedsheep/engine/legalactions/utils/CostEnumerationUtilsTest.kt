package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [CostEnumerationUtils] — a pure utility called by every
 * enumerator (Cast, CastFromZone, ActivatedAbility, Cycling, Morph, …).
 *
 * Phase 6 of the legalactions coverage work. Each test exercises one helper
 * method directly on a seeded game state, avoiding the full enumerator
 * pipeline so the unit under test stays narrow.
 *
 * Not covered (left to a follow-up): the Morph-specific helpers
 * (`findMorphExileTargets`, `findMorphDiscardTargets`, `findReturnToHandTargets`)
 * and `canPayTapAttachedCreatureCost` — these are simple delegations to the
 * battlefield/hand filter pattern already exercised here.
 */
class CostEnumerationUtilsTest : FunSpec({

    /**
     * Build a [CostEnumerationUtils] backed by the driver's registry.
     * Sharing instances across a single test is fine — the utility is stateless.
     */
    fun utils(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver): CostEnumerationUtils {
        val registry = driver.game.cardRegistry
        val predicateEvaluator = PredicateEvaluator()
        return CostEnumerationUtils(
            manaSolver = ManaSolver(registry),
            costCalculator = CostCalculator(registry),
            predicateEvaluator = predicateEvaluator,
            cardRegistry = registry
        )
    }

    /** Battlefield entity id for the P1 permanent matching [name]. */
    fun entityOnBattlefield(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getBattlefield(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    // -------------------------------------------------------------------------
    context("Battlefield target finders") {

        test("findAbilityTapTargets returns only untapped creatures matching the filter") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears", "Grizzly Bears", "Forest")
            )
            // Tap one of the Bears.
            val bears = driver.game.state.getBattlefield(driver.player1).filter { id ->
                driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val tapped = driver.game.state.getEntity(bears[0])!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(bears[0], tapped))

            val targets = utils(driver).findAbilityTapTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature
            )

            targets shouldHaveSize 1
            targets.single() shouldBe bears[1]  // only the untapped one
        }

        test("findAbilitySacrificeTargets excludes the excluded entity id") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears", "Grizzly Bears")
            )
            val bears = driver.game.state.getBattlefield(driver.player1)
            val excluded = bears.first()

            val targets = utils(driver).findAbilitySacrificeTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature, excluded
            )

            targets shouldHaveSize 1
            targets shouldNotContainExcluded excluded
        }

        test("findAbilityBounceTargets returns all permanents matching filter (tapped or not)") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears", "Forest")
            )
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")
            // Tap the Bears — bounce should still pick it up (filter doesn't care).
            val tapped = driver.game.state.getEntity(bearsId)!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(bearsId, tapped))

            val targets = utils(driver).findAbilityBounceTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature
            )

            targets shouldHaveSize 1
            targets.single() shouldBe bearsId
        }

        test("findAbilityTapTargets returns empty when nothing matches the filter") {
            val driver = setupP1(
                battlefield = listOf("Forest", "Forest")  // no creatures
            )

            utils(driver).findAbilityTapTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature
            ).shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    context("Zone-based exile finders") {

        test("findExileTargets from GRAVEYARD returns matching graveyard cards") {
            val driver = setupP1(
                graveyard = listOf("Grizzly Bears", "Forest", "Grizzly Bears")
            )

            val targets = utils(driver).findExileTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature, CostZone.GRAVEYARD
            )

            targets shouldHaveSize 2  // two Grizzly Bears, Forest excluded
        }

        test("findExileTargets from HAND returns matching hand cards") {
            val driver = setupP1(
                hand = listOf("Grizzly Bears", "Forest")
            )

            val targets = utils(driver).findExileTargets(
                driver.game.state, driver.player1, GameObjectFilter.Creature, CostZone.HAND
            )

            targets shouldHaveSize 1
            driver.game.state.getEntity(targets.single())?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
        }
    }

    // -------------------------------------------------------------------------
    context("Convoke") {

        test("findConvokeCreatures excludes tapped and non-creature permanents") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears", "Grizzly Bears", "Forest")
            )
            // Tap one Bears.
            val bears = driver.game.state.getBattlefield(driver.player1).filter { id ->
                driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val tappedBears = driver.game.state.getEntity(bears[0])!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(bears[0], tappedBears))

            val convoke = utils(driver).findConvokeCreatures(driver.game.state, driver.player1)

            convoke shouldHaveSize 1  // only the untapped Bears — Forest filtered by isCreature
            convoke.single().entityId shouldBe bears[1]
            convoke.single().name shouldBe "Grizzly Bears"
        }

        test("canAffordWithConvoke: enough total resources to pay {2} with 2 creatures, no mana") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears", "Grizzly Bears")  // no lands
            )
            val u = utils(driver)
            val convoke = u.findConvokeCreatures(driver.game.state, driver.player1)

            val affordable = u.canAffordWithConvoke(
                driver.game.state, driver.player1, ManaCost.parse("{2}"), convoke
            )

            affordable shouldBe true
        }

        test("canAffordWithConvoke: insufficient total resources is not affordable") {
            val driver = setupP1(
                battlefield = listOf("Grizzly Bears")  // only 1 creature, no lands
            )
            val u = utils(driver)
            val convoke = u.findConvokeCreatures(driver.game.state, driver.player1)

            val affordable = u.canAffordWithConvoke(
                driver.game.state, driver.player1, ManaCost.parse("{3}"), convoke
            )

            affordable shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    context("Delve") {

        test("findDelveCards returns every graveyard card with name populated") {
            val driver = setupP1(
                graveyard = listOf("Grizzly Bears", "Forest")
            )

            val delve = utils(driver).findDelveCards(driver.game.state, driver.player1)

            delve shouldHaveSize 2
            delve.map { it.name }.toSet() shouldBe setOf("Grizzly Bears", "Forest")
        }

        test("calculateMinDelveNeeded returns 0 when cost is already affordable without delve") {
            val driver = setupP1(
                battlefield = listOf("Forest", "Forest"),  // can pay {2}
                graveyard = listOf("Grizzly Bears", "Forest")
            )
            val u = utils(driver)
            val delve = u.findDelveCards(driver.game.state, driver.player1)

            val min = u.calculateMinDelveNeeded(
                driver.game.state, driver.player1, ManaCost.parse("{2}"), delve
            )

            min shouldBe 0
        }

        test("calculateMinDelveNeeded returns the minimum graveyard cards needed to afford") {
            val driver = setupP1(
                battlefield = listOf("Forest"),  // 1 mana — can't pay {3} without delve
                graveyard = listOf("Grizzly Bears", "Forest", "Grizzly Bears")  // 3 delve-able
            )
            val u = utils(driver)
            val delve = u.findDelveCards(driver.game.state, driver.player1)

            val min = u.calculateMinDelveNeeded(
                driver.game.state, driver.player1, ManaCost.parse("{3}"), delve
            )

            // 1 Forest pays {1}, need 2 more generic → 2 delve exiles.
            min shouldBe 2
        }
    }

    // -------------------------------------------------------------------------
    context("canPayTapCost") {

        test("returns false when the source is already tapped") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears"))
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")
            val tapped = driver.game.state.getEntity(bearsId)!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(bearsId, tapped))

            utils(driver).canPayTapCost(driver.game.state, bearsId) shouldBe false
        }

        test("returns false when a creature has summoning sickness and no haste") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears"))
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")
            val sick = driver.game.state.getEntity(bearsId)!!.with(SummoningSicknessComponent)
            driver.game.replaceState(driver.game.state.withEntity(bearsId, sick))

            utils(driver).canPayTapCost(driver.game.state, bearsId) shouldBe false
        }

        test("returns true for an untapped creature with no summoning sickness") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears"))
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")

            utils(driver).canPayTapCost(driver.game.state, bearsId) shouldBe true
        }

        test("returns true for a land even with summoning sickness (skip-for-lands branch)") {
            val driver = setupP1(battlefield = listOf("Forest"))
            val forestId = entityOnBattlefield(driver, "Forest")
            // Attach summoning sickness; canPayTapCost must ignore it for lands.
            val sick = driver.game.state.getEntity(forestId)!!.with(SummoningSicknessComponent)
            driver.game.replaceState(driver.game.state.withEntity(forestId, sick))

            utils(driver).canPayTapCost(driver.game.state, forestId) shouldBe true
        }
    }

    // -------------------------------------------------------------------------
    context("buildCounterRemovalCreatures") {

        test("returns only creatures with +1/+1 counters, tagged with the available count") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears", "Grizzly Bears"))
            val bears = driver.game.state.getBattlefield(driver.player1).toList()
            // Put 3 +1/+1 counters on the first Bears; leave the second without any.
            val boosted = driver.game.state.getEntity(bears[0])!!.let { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
            }
            driver.game.replaceState(driver.game.state.withEntity(bears[0], boosted))

            val creatures = utils(driver).buildCounterRemovalCreatures(driver.game.state, driver.player1)

            creatures shouldHaveSize 1
            creatures.single().entityId shouldBe bears[0]
            creatures.single().availableCounters shouldBe 3
        }

        test("returns empty when no creature has +1/+1 counters") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears", "Forest"))

            utils(driver).buildCounterRemovalCreatures(driver.game.state, driver.player1).shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    context("calculateMaxAffordableX") {

        test("caps X by available mana when the mana cost has X") {
            val driver = setupP1(battlefield = listOf("Forest", "Forest", "Forest"))
            // Cost {X}{G}: fixed cost 1 (the {G}), available sources 3, so max X = 2.
            val u = utils(driver)
            val manaCost = ManaCost.parse("{X}{G}")

            val maxX = u.calculateMaxAffordableX(
                driver.game.state,
                driver.player1,
                AbilityCost.Mana(manaCost),
                manaCost
            )

            maxX shouldBe 2
        }

        test("caps X by graveyard size when cost includes ExileXFromGraveyard") {
            val driver = setupP1(
                battlefield = listOf("Forest", "Forest", "Forest", "Forest", "Forest"),
                graveyard = listOf("Grizzly Bears", "Forest")  // 2 cards
            )
            val manaCost = ManaCost.parse("{X}")
            val compositeCost = AbilityCost.Composite(listOf(
                AbilityCost.Mana(manaCost),
                AbilityCost.ExileXFromGraveyard()
            ))

            val maxX = utils(driver).calculateMaxAffordableX(
                driver.game.state, driver.player1, compositeCost, manaCost
            )

            // Mana allows X up to 5 (5 lands, 0 fixed), but graveyard has only 2 cards.
            maxX shouldBe 2
        }

        test("caps X by total +1/+1 counters for RemoveXPlusOnePlusOneCounters") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears"))
            val bearsId = entityOnBattlefield(driver, "Grizzly Bears")
            val boosted = driver.game.state.getEntity(bearsId)!!.let { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent())
                    .withAdded(CounterType.PLUS_ONE_PLUS_ONE, 4))
            }
            driver.game.replaceState(driver.game.state.withEntity(bearsId, boosted))

            val maxX = utils(driver).calculateMaxAffordableX(
                driver.game.state,
                driver.player1,
                AbilityCost.RemoveXPlusOnePlusOneCounters,
                manaCost = null
            )

            maxX shouldBe 4
        }

        test("caps X by untapped matching permanents for TapXPermanents") {
            val driver = setupP1(battlefield = listOf("Grizzly Bears", "Grizzly Bears", "Grizzly Bears"))
            // Tap one Bears — only 2 remain tappable.
            val bears = driver.game.state.getBattlefield(driver.player1).filter { id ->
                driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val tapped = driver.game.state.getEntity(bears[0])!!.with(TappedComponent)
            driver.game.replaceState(driver.game.state.withEntity(bears[0], tapped))

            val tapXCost = AbilityCost.TapXPermanents(filter = GameObjectFilter.Creature)

            val maxX = utils(driver).calculateMaxAffordableX(
                driver.game.state, driver.player1, tapXCost, manaCost = null
            )

            maxX shouldBe 2
        }
    }

})

// --- local matcher -----------------------------------------------------------

private infix fun List<EntityId>.shouldNotContainExcluded(excluded: EntityId): List<EntityId> =
    apply { (excluded !in this) shouldBe true }
