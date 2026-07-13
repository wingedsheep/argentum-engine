package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.SpringLoadedSawblades
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Spring-Loaded Sawblades // Bladewheel Chariot (LCI #36) — Craft transform DFC (CR 702.167).
 *
 * Front — {1}{W} Artifact:
 *  - Flash; ETB "it deals 5 damage to target tapped creature an opponent controls".
 *  - Craft with artifact {3}{W} (exactly one artifact material).
 * Back — Bladewheel Chariot, Artifact — Vehicle 5/5:
 *  - "Tap two other untapped artifacts you control: This Vehicle becomes an artifact creature
 *     until end of turn." (excludeSelf tap cost)
 *  - Crew 1.
 *
 * Covers: (1) instant-speed cast (flash) + targeted ETB damage killing a tapped opposing
 * creature, with the negative that an untapped creature is not a legal target; (2) craft
 * end-to-end — material exiled, source returns transformed as a non-creature Vehicle;
 * (3) the tap-two-artifacts self-animate — both artifacts tapped, Chariot becomes a 5/5
 * artifact creature (projected), and the Chariot itself can't be counted among the two
 * (excludeSelf); (4) Crew 1 also animates it; (5) craft refuses a creature (non-artifact)
 * material.
 */
class SpringLoadedSawbladesScenarioTest : FunSpec({

    // Opponent's 3/3 victim — 5 ETB damage is lethal.
    val testOx = CardDefinition.creature(
        name = "Sawblades Test Ox",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Ox")),
        power = 3,
        toughness = 3,
    )

    // Plain artifact: craft material / tap-cost fodder.
    val testRelic = card("Sawblades Test Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    val projector = StateProjector()

    // Front face's only activated ability is the Craft; the ETB strike is a triggered ability.
    val craftAbilityId = SpringLoadedSawblades.activatedAbilities.single().id

    // Back face's only activated ability is the tap-two-artifacts self-animate.
    val chariotAnimateAbilityId = SpringLoadedSawblades.backFace!!.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testOx, testRelic))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        return driver
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    /**
     * Craft the Sawblades on [player]'s battlefield into Bladewheel Chariot, exiling a test
     * relic as the single artifact material. Assumes a main phase with an empty stack.
     * Returns the entity id (now the Chariot).
     */
    fun craftChariot(driver: GameTestDriver, player: EntityId): EntityId {
        val sawblades = driver.putPermanentOnBattlefield(player, "Spring-Loaded Sawblades")
        val material = driver.putPermanentOnBattlefield(player, "Sawblades Test Relic")
        driver.giveMana(player, Color.WHITE, 4) // {3}{W}
        driver.submitSuccess(
            ActivateAbility(
                playerId = player,
                sourceId = sawblades,
                abilityId = craftAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(material))
            )
        )
        driver.bothPass() // resolve the craft ability
        return sawblades
    }

    test("flash: cast at instant speed; ETB deals 5 damage to target tapped opposing creature; untapped creature is not a legal target") {
        val driver = setup()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tappedOx = driver.putCreatureOnBattlefield(opponent, "Sawblades Test Ox")
        driver.tapPermanent(tappedOx)
        val untappedOx = driver.putCreatureOnBattlefield(opponent, "Sawblades Test Ox")

        val sawblades = driver.putCardInHand(active, "Spring-Loaded Sawblades")

        // End step of the active player's own turn: not a main phase, so only an
        // instant-speed cast is legal — flash is what makes this succeed. Mana is
        // given after the step change (pools empty at step boundaries, CR 500.4).
        driver.passPriorityUntil(Step.END)
        driver.giveMana(active, Color.WHITE, 2) // {1}{W}
        val castResult = driver.castSpell(active, sawblades, emptyList())
        castResult.error shouldBe null

        // Resolve the artifact; its ETB trigger asks for a target when it goes on the stack.
        var guard = 0
        while (driver.pendingDecision == null && guard++ < 20) driver.bothPass()
        driver.pendingDecision.shouldNotBeNull()

        // Negative: the UNTAPPED creature is not a legal target for the trigger.
        driver.submitTargetSelection(active, listOf(untappedOx)).isSuccess shouldBe false

        // The tapped creature is legal; 5 damage kills the 3/3.
        driver.submitTargetSelection(active, listOf(tappedOx)).isSuccess shouldBe true
        driver.drainStack()

        // 5 damage kills the tapped 3/3.
        (tappedOx in driver.state.getBattlefield()) shouldBe false
        // The untapped bystander is untouched.
        (untappedOx in driver.state.getBattlefield()) shouldBe true

        // The Sawblades itself is on the battlefield as its front face.
        val card = driver.state.getEntity(sawblades)!!.get<CardComponent>()!!
        card.name shouldBe "Spring-Loaded Sawblades"
    }

    test("craft with artifact {3}{W}: exiles the material and returns transformed as Bladewheel Chariot, a non-creature Vehicle") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sawblades = driver.putPermanentOnBattlefield(p1, "Spring-Loaded Sawblades")
        val material = driver.putPermanentOnBattlefield(p1, "Sawblades Test Relic")
        driver.giveMana(p1, Color.WHITE, 4)

        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = sawblades,
                abilityId = craftAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(material))
            )
        )
        driver.bothPass()

        // Material exiled.
        (material in driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(p1, Zone.EXILE))) shouldBe true

        // Source returned to the battlefield as the back face.
        val container = driver.state.getEntity(sawblades)
        container.shouldNotBeNull()
        val card = container.get<CardComponent>()
        card.shouldNotBeNull()
        card.name shouldBe "Bladewheel Chariot"
        card.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT)
        (Subtype.VEHICLE in card.typeLine.subtypes) shouldBe true

        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // A Vehicle is not a creature until crewed/animated.
        val projected = projector.project(driver.state)
        projected.isCreature(sawblades) shouldBe false
    }

    test("tap two other untapped artifacts: both tapped, Chariot becomes a 5/5 artifact creature; the Chariot can't count itself") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chariot = craftChariot(driver, p1)
        val relicA = driver.putPermanentOnBattlefield(p1, "Sawblades Test Relic")
        val relicB = driver.putPermanentOnBattlefield(p1, "Sawblades Test Relic")

        // excludeSelf: the Chariot (itself an untapped artifact) is not a legal tap choice.
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = chariot,
                abilityId = chariotAnimateAbilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(chariot, relicA))
            )
        ).isSuccess shouldBe false

        // Two OTHER untapped artifacts pay the cost.
        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = chariot,
                abilityId = chariotAnimateAbilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(relicA, relicB))
            )
        )
        driver.isTapped(relicA) shouldBe true
        driver.isTapped(relicB) shouldBe true

        driver.bothPass() // resolve the animate

        val projected = projector.project(driver.state)
        projected.isCreature(chariot) shouldBe true
        projected.hasType(chariot, "ARTIFACT") shouldBe true
        projected.hasSubtype(chariot, "Vehicle") shouldBe true
        projected.getPower(chariot) shouldBe 5
        projected.getToughness(chariot) shouldBe 5
    }

    test("crew 1 animates Bladewheel Chariot") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val chariot = craftChariot(driver, p1)
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears") // power 2 >= crew 1

        driver.submitSuccess(CrewVehicle(p1, chariot, listOf(bear)))
        driver.isTapped(bear) shouldBe true
        driver.bothPass() // resolve the crew animate

        val projected = projector.project(driver.state)
        projected.isCreature(chariot) shouldBe true
        projected.getPower(chariot) shouldBe 5
        projected.getToughness(chariot) shouldBe 5
    }

    test("craft rejects a creature (non-artifact) material") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sawblades = driver.putPermanentOnBattlefield(p1, "Spring-Loaded Sawblades")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.WHITE, 4)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = sawblades,
                abilityId = craftAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(bear))
            )
        )
        result.isSuccess shouldBe false

        // Still the front face on the battlefield; the bear is untouched.
        driver.state.getEntity(sawblades)!!.get<CardComponent>()!!.name shouldBe "Spring-Loaded Sawblades"
        (bear in driver.state.getBattlefield()) shouldBe true
    }
})
