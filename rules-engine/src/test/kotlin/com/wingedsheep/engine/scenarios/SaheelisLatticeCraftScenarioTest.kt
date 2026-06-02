package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.SaheelisLattice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for the Craft mechanic (CR 702.167), exercised end-to-end via
 * Saheeli's Lattice // Mastercraft Raptor.
 *
 * Covers:
 *  - Cost: pays mana, exiles self, exiles ≥1 Dinosaur material from the combined
 *    battlefield-controlled / graveyard pool (CR 702.167a-b).
 *  - Effect resolution: source returns to battlefield as its back face under owner's
 *    control with a [CraftedFromExiledComponent] recording the materials.
 *  - Back-face *-power CDA (CR 702.167c): Mastercraft Raptor's projected power equals
 *    the total printed power of the exiled materials.
 *  - Timing: "Activate only as a sorcery" — rejected outside main phase / non-empty stack.
 *  - Validation: rejected when no Dinosaur material is available.
 */
class SaheelisLatticeCraftScenarioTest : FunSpec({

    // Test materials — distinct printed powers so totals tell us which were used.
    val smallDino = CardDefinition.creature(
        name = "Test Tiny Dino",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.DINOSAUR),
        power = 1,
        toughness = 1,
        oracleText = ""
    )
    val bigDino = CardDefinition.creature(
        name = "Test Big Dino",
        manaCost = ManaCost.parse("{4}{R}"),
        subtypes = setOf(Subtype.DINOSAUR),
        power = 5,
        toughness = 4,
        oracleText = ""
    )
    val nonDino = CardDefinition.creature(
        name = "Test Non-Dino",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 2,
        toughness = 1,
        oracleText = ""
    )

    val projector = StateProjector()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SaheelisLattice, smallDino, bigDino, nonDino))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            skipMulligans = true
        )
        return driver
    }

    // The front face holds only one activated ability — the Craft. (The ETB rummage is a
    // triggered ability and lives in a separate list.)
    fun craftAbilityId() = SaheelisLattice.activatedAbilities.single().id

    test("pays mana, exiles self and a battlefield Dinosaur, returns as Mastercraft Raptor with power = exiled Dino power") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        val dino = driver.putCreatureOnBattlefield(p1, "Test Big Dino")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 5)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(dino))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability on the stack.
        driver.bothPass()

        // Material exiled.
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(p1, Zone.EXILE)).shouldContain(dino)

        // Source returned to battlefield as back face.
        val saheeliContainer = driver.state.getEntity(saheeli)
        saheeliContainer.shouldNotBeNull()
        val card = saheeliContainer.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Mastercraft Raptor"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)

        val dfc = saheeliContainer.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // CraftedFromExiledComponent re-attached on entry with the chosen material.
        val crafted = saheeliContainer.get<CraftedFromExiledComponent>()
        crafted.shouldNotBeNull()
        crafted.exiledIds shouldBe listOf(dino)

        // Projected power = total power of exiled cards used to craft it = 5.
        val projected = projector.project(driver.state)
        projected.getPower(saheeli) shouldBe 5
        projected.getToughness(saheeli) shouldBe 4
    }

    test("materials may be selected across battlefield and graveyard simultaneously (CR 702.167b)") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        val battlefieldDino = driver.putCreatureOnBattlefield(p1, "Test Tiny Dino")    // 1 power
        val graveyardDino = driver.putCardInGraveyard(p1, "Test Big Dino")             // 5 power
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 5)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(battlefieldDino, graveyardDino))
            )
        )
        driver.bothPass()

        val exile = driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(p1, Zone.EXILE))
        exile.shouldContainExactlyInAnyOrder(battlefieldDino, graveyardDino)

        val crafted = driver.state.getEntity(saheeli)!!.get<CraftedFromExiledComponent>()!!
        crafted.exiledIds.toSet() shouldBe setOf(battlefieldDino, graveyardDino)

        // Projected power = 1 + 5 = 6.
        val projected = projector.project(driver.state)
        projected.getPower(saheeli) shouldBe 6
    }

    test("rejects activation when the activator supplies no chosen materials even though candidates exist") {
        // Materials are a player choice (CR 702.167a-b). With at least one valid Dinosaur on
        // the battlefield the ability is *legal* to activate, but the activator must still
        // explicitly pick the materials — the engine must not auto-pick silently.
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        driver.putCreatureOnBattlefield(p1, "Test Big Dino")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 5)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId()
                // costPayment intentionally omitted
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull() shouldContain "Craft"
    }

    test("rejects activation when no Dinosaur material is available") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        driver.putCreatureOnBattlefield(p1, "Test Non-Dino")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 5)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId()
            )
        )
        result.isSuccess shouldBe false
    }

    test("Mastercraft Raptor reverts to Saheeli's Lattice (front face) when it leaves the battlefield (CR 712.8a)") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        val dino = driver.putCreatureOnBattlefield(p1, "Test Big Dino")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 5)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(dino))
            )
        )
        driver.bothPass()

        // Sanity: the source is on the battlefield as Mastercraft Raptor.
        driver.state.getEntity(saheeli)!!.get<CardComponent>()!!.name shouldBe "Mastercraft Raptor"

        // Send Mastercraft Raptor to the graveyard (any leave-battlefield event will do).
        val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = saheeli,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(transition.state)

        // Per CR 712.8a, in any non-battlefield/non-stack zone the DFC has only the
        // characteristics of its front face — so the card in the graveyard is Saheeli's
        // Lattice, not Mastercraft Raptor.
        val graveyardContainer = driver.state.getEntity(saheeli)
        graveyardContainer.shouldNotBeNull()
        val graveyardCard = graveyardContainer.get<CardComponent>()
        graveyardCard.shouldNotBeNull()
        graveyardCard.name shouldBe "Saheeli's Lattice"

        val dfc = graveyardContainer.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.FRONT
    }

    test("rejects activation at instant speed (CR 702.167a: \"Activate only as a sorcery\")") {
        // Use UPKEEP so the active player has priority but it is *not* a main phase — that's
        // the cleanest "instant speed but not sorcery speed" canvas (combat steps also have
        // stack-empty subtleties that conflate the test).
        val driver = setup()
        val p1 = driver.activePlayer!!

        val saheeli = driver.putPermanentOnBattlefield(p1, "Saheeli's Lattice")
        driver.putCreatureOnBattlefield(p1, "Test Tiny Dino")
        driver.giveMana(p1, Color.RED, 5)
        driver.passPriorityUntil(Step.UPKEEP)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = saheeli,
                abilityId = craftAbilityId()
            )
        )
        result.isSuccess shouldBe false
        result.error.shouldNotBeNull() shouldContain "sorcery"
    }
})

private fun List<com.wingedsheep.sdk.model.EntityId>.shouldContain(id: com.wingedsheep.sdk.model.EntityId) {
    if (id !in this) throw AssertionError("Expected zone to contain $id, but was $this")
}
