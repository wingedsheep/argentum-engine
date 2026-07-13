package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.WaterloggedHulk
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Waterlogged Hulk // Watertight Gondola — {U} Artifact // Artifact — Vehicle 4/4 (LCI #83).
 *
 * Front face:
 *  - {T}: Mill a card.
 *  - Craft with Island {3}{U} (exactly one Island you control or Island card in your
 *    graveyard, CR 702.167a-b; sorcery-only).
 *
 * Back face — Watertight Gondola:
 *  - Vigilance, Crew 1.
 *  - Descend 8 — can't be blocked as long as there are eight or more permanent cards
 *    in your graveyard (ConditionalStaticAbility gate on CantBeBlocked).
 */
class WaterloggedHulkScenarioTest : FunSpec({

    val projector = StateProjector()

    val millAbilityId = WaterloggedHulk.activatedAbilities.single { it.cost == AbilityCost.Tap }.id
    val craftAbilityId = WaterloggedHulk.activatedAbilities.single { it.cost != AbilityCost.Tap }.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    /** Crafts the hulk using [material], resolving the ability; returns the (transformed) entity. */
    fun craftGondola(driver: GameTestDriver, me: EntityId, hulk: EntityId, material: EntityId) {
        driver.giveMana(me, Color.BLUE, 4)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = hulk,
                abilityId = craftAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(material))
            )
        )
        driver.bothPass()
    }

    // -------------------------------------------------------------------------
    // Front face — {T}: Mill a card
    // -------------------------------------------------------------------------

    test("front face: {T}: Mill a card puts the top card of your library into your graveyard") {
        val driver = createDriver()
        val me = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hulk = driver.putPermanentOnBattlefield(me, "Waterlogged Hulk")
        // A recognizable top card so we know exactly what got milled.
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = hulk, abilityId = millAbilityId))
        driver.bothPass()

        withClue("tap cost paid") {
            driver.isTapped(hulk) shouldBe true
        }
        withClue("the top card was milled into the graveyard") {
            driver.getGraveyardCardNames(me) shouldBe listOf("Grizzly Bears")
        }
    }

    // -------------------------------------------------------------------------
    // Craft with Island {3}{U}
    // -------------------------------------------------------------------------

    test("craft with an Island you control exiles it and returns the source transformed as Watertight Gondola (not a creature until crewed)") {
        val driver = createDriver()
        val me = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hulk = driver.putPermanentOnBattlefield(me, "Waterlogged Hulk")
        val island = driver.putPermanentOnBattlefield(me, "Island")

        craftGondola(driver, me, hulk, island)

        withClue("the Island material was exiled") {
            driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain island
        }

        val container = driver.state.getEntity(hulk)
        container.shouldNotBeNull()
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Watertight Gondola"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT)
        (Subtype.VEHICLE in card.typeLine.subtypes) shouldBe true

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        withClue("an uncrewed Vehicle is not a creature") {
            projector.project(driver.state).isCreature(hulk) shouldBe false
        }
    }

    // -------------------------------------------------------------------------
    // Back face — Crew 1, vigilance, Descend 8 unblockable
    // -------------------------------------------------------------------------

    test("crew 1 makes it a 4/4 artifact creature with vigilance; with eight or more permanent cards in the graveyard it can't be blocked") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hulk = driver.putPermanentOnBattlefield(me, "Waterlogged Hulk")
        // Nine Island cards in the graveyard; crafting exiles one from there (a graveyard
        // Island is a legal material, CR 702.167b), leaving exactly eight permanent cards
        // — the Descend 8 threshold.
        repeat(9) { driver.putCardInGraveyard(me, "Island") }
        val graveyardIsland = driver.getGraveyard(me).first()

        craftGondola(driver, me, hulk, graveyardIsland)
        driver.getGraveyard(me).size shouldBe 8

        driver.removeSummoningSickness(hulk)
        val crewer = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.submitSuccess(CrewVehicle(me, hulk, listOf(crewer)))
        driver.bothPass() // the crew animate goes on the stack — resolve it

        val projected = projector.project(driver.state)
        withClue("crewed Vehicle is a 4/4 artifact creature with vigilance") {
            projected.isCreature(hulk) shouldBe true
            projected.getPower(hulk) shouldBe 4
            projected.getToughness(hulk) shouldBe 4
            projected.hasKeyword(hulk, Keyword.VIGILANCE) shouldBe true
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(hulk), opponent).error shouldBe null
        withClue("vigilance: attacking doesn't tap it") {
            driver.isTapped(hulk) shouldBe false
        }

        driver.bothPass()
        withClue("Descend 8 met (8 permanent cards) → can't be blocked") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(hulk))).error shouldNotBe null
        }
    }

    test("with fewer than eight permanent cards in the graveyard it can be blocked") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hulk = driver.putPermanentOnBattlefield(me, "Waterlogged Hulk")
        val island = driver.putPermanentOnBattlefield(me, "Island")
        // Seven permanent cards — one short of Descend 8. The material comes from the
        // battlefield, so the graveyard count is untouched by the craft.
        repeat(7) { driver.putCardInGraveyard(me, "Island") }

        craftGondola(driver, me, hulk, island)
        driver.getGraveyard(me).size shouldBe 7

        driver.removeSummoningSickness(hulk)
        val crewer = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.submitSuccess(CrewVehicle(me, hulk, listOf(crewer)))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(hulk), opponent).error shouldBe null
        driver.bothPass()
        withClue("Descend 8 unmet (7 permanent cards) → can be blocked") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(hulk))).error shouldBe null
        }
    }

    // -------------------------------------------------------------------------
    // Negative — illegal craft materials
    // -------------------------------------------------------------------------

    test("craft rejects a non-Island land and rejects two Islands (exactly one material)") {
        val driver = createDriver()
        val me = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hulk = driver.putPermanentOnBattlefield(me, "Waterlogged Hulk")
        val plains = driver.putPermanentOnBattlefield(me, "Plains")
        val island1 = driver.putPermanentOnBattlefield(me, "Island")
        val island2 = driver.putPermanentOnBattlefield(me, "Island")
        driver.giveMana(me, Color.BLUE, 4)

        withClue("a Plains is not an Island — rejected as craft material") {
            driver.submit(
                ActivateAbility(
                    playerId = me,
                    sourceId = hulk,
                    abilityId = craftAbilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(plains))
                )
            ).isSuccess shouldBe false
        }

        withClue("Craft with Island takes exactly one material — two Islands rejected") {
            driver.submit(
                ActivateAbility(
                    playerId = me,
                    sourceId = hulk,
                    abilityId = craftAbilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(island1, island2))
                )
            ).isSuccess shouldBe false
        }

        withClue("sanity: a single Island is accepted") {
            driver.submit(
                ActivateAbility(
                    playerId = me,
                    sourceId = hulk,
                    abilityId = craftAbilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(island1))
                )
            ).isSuccess shouldBe true
        }
    }
})
