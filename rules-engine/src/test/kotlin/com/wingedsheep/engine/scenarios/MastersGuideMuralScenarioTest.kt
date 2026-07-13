package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.MastersGuideMural
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Master's Guide-Mural // Master's Manufactory (LCI #233).
 *
 * Covers:
 *  - Front-face ETB (cast from hand): creates a 4/4 white and blue Golem artifact creature token.
 *  - Craft with artifact {4}{W}{W}{U} (CR 702.167): exiles self + exactly one artifact material,
 *    returns transformed as Master's Manufactory.
 *  - Back-face {T} ability the SAME turn it was crafted: the craft return itself is an artifact
 *    that entered the battlefield under your control this turn ("this artifact or another
 *    artifact"), and a noncreature artifact's {T} ability is not restricted by summoning
 *    sickness (CR 302.6 applies to creatures only).
 *  - Activation restriction: on a later turn with no artifact having entered, activation is
 *    rejected; after casting an artifact that turn, activation succeeds.
 *  - Negative: a non-artifact (creature) craft material is rejected.
 */
class MastersGuideMuralScenarioTest : FunSpec({

    // A cheap plain artifact — craft material / entered-this-turn enabler.
    val testTrinket = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    // A non-artifact creature — illegal craft material.
    val testBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val projector = StateProjector()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testTrinket, testBear))
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            skipMulligans = true
        )
        return driver
    }

    // The front face's only activated ability is the Craft.
    fun craftAbilityId() = MastersGuideMural.activatedAbilities.single().id

    // The back face's only activated ability is the {T} token maker.
    fun manufactoryAbilityId() = MastersGuideMural.backFace!!.activatedAbilities.single().id

    fun GameTestDriver.golemsOf(playerId: EntityId): List<EntityId> =
        state.getBattlefield().filter { id ->
            // CreateTokenExecutor names tokens "<creature types> Token" by default.
            state.getEntity(id)?.get<CardComponent>()?.name == "Golem Token" && getController(id) == playerId
        }

    /** Craft the Mural with a Test Trinket material during p1's precombat main; returns the entity id. */
    fun GameTestDriver.craftMural(p1: EntityId): EntityId {
        val mural = putPermanentOnBattlefield(p1, "Master's Guide-Mural")
        val trinket = putPermanentOnBattlefield(p1, "Test Trinket")
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(p1, Color.WHITE, 2)
        giveMana(p1, Color.BLUE, 1)
        giveColorlessMana(p1, 4)
        val result = submit(
            ActivateAbility(
                playerId = p1,
                sourceId = mural,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(trinket))
            )
        )
        withClue("Craft activation should succeed") { result.error shouldBe null }
        bothPass() // resolve the craft ability
        // Material exiled + returned transformed.
        state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContain trinket
        state.getEntity(mural)!!.get<CardComponent>()!!.name shouldBe "Master's Manufactory"
        return mural
    }

    test("cast from hand: ETB trigger creates one 4/4 white and blue Golem artifact creature token") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mural = driver.putCardInHand(p1, "Master's Guide-Mural")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 3)

        val cast = driver.castSpell(p1, mural)
        withClue("Casting Master's Guide-Mural should succeed") { cast.error shouldBe null }
        driver.bothPass() // resolve the artifact spell
        driver.bothPass() // resolve the ETB trigger

        val golems = driver.golemsOf(p1)
        golems.size shouldBe 1
        val golem = golems.single()

        val golemCard = driver.state.getEntity(golem)?.get<CardComponent>()
        golemCard.shouldNotBeNull()
        golemCard.colors shouldBe setOf(Color.WHITE, Color.BLUE)
        golemCard.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
        golemCard.typeLine.subtypes shouldContain Subtype("Golem")

        val projected = projector.project(driver.state)
        projected.getPower(golem) shouldBe 4
        projected.getToughness(golem) shouldBe 4
    }

    test("craft with artifact: exiles the material, returns transformed, and the {T} ability works the same turn") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val mural = driver.craftMural(p1)

        // Back face on the battlefield: Master's Manufactory, a noncreature Artifact.
        val container = driver.state.getEntity(mural)
        container.shouldNotBeNull()
        container.get<CardComponent>()!!.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT)
        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // The Manufactory itself entered the battlefield under p1's control this turn (via the
        // craft return), satisfying "this artifact or another artifact entered ...". As a
        // noncreature artifact it has no summoning-sickness restriction on {T} (CR 302.6), so
        // the ability is activatable immediately.
        val activate = driver.submit(
            ActivateAbility(playerId = p1, sourceId = mural, abilityId = manufactoryAbilityId())
        )
        withClue("Same-turn activation should succeed") { activate.error shouldBe null }
        driver.bothPass() // resolve the token ability

        driver.state.getEntity(mural)?.has<TappedComponent>() shouldBe true
        val golems = driver.golemsOf(p1)
        golems.size shouldBe 1
        val projected = projector.project(driver.state)
        projected.getPower(golems.single()) shouldBe 4
        projected.getToughness(golems.single()) shouldBe 4
    }

    test("activation restriction: rejected on a later turn with no artifact entered; allowed after one enters") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val mural = driver.craftMural(p1)

        // Advance whole turns until it is p1's turn again — the entered-this-turn tracker is
        // cleared at each cleanup, and no artifact enters in between.
        do {
            driver.passPriorityUntil(Step.END)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != p1)

        val rejected = driver.submit(
            ActivateAbility(playerId = p1, sourceId = mural, abilityId = manufactoryAbilityId())
        )
        withClue("No artifact entered this turn — activation must be rejected") {
            rejected.isSuccess shouldBe false
        }
        driver.golemsOf(p1).size shouldBe 0

        // Cast an artifact this turn (casting routes through the real entry pipeline, which
        // records the per-turn "artifact entered under your control" tracker).
        val trinket = driver.putCardInHand(p1, "Test Trinket")
        driver.giveColorlessMana(p1, 1)
        val cast = driver.castSpell(p1, trinket)
        withClue("Casting Test Trinket should succeed") { cast.error shouldBe null }
        driver.bothPass() // resolve the trinket

        val allowed = driver.submit(
            ActivateAbility(playerId = p1, sourceId = mural, abilityId = manufactoryAbilityId())
        )
        withClue("An artifact entered this turn — activation should now succeed") {
            allowed.error shouldBe null
        }
        driver.bothPass() // resolve the token ability
        driver.golemsOf(p1).size shouldBe 1
    }

    test("craft rejects a non-artifact (creature) material") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        val mural = driver.putPermanentOnBattlefield(p1, "Master's Guide-Mural")
        val bear = driver.putCreatureOnBattlefield(p1, "Test Bear")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.WHITE, 2)
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = mural,
                abilityId = craftAbilityId(),
                costPayment = AdditionalCostPayment(exiledCards = listOf(bear))
            )
        )
        result.isSuccess shouldBe false
    }
})
