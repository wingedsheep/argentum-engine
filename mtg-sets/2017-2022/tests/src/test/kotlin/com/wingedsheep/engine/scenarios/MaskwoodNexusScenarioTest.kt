package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.MaskwoodNexus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val projector = StateProjector()
private val predicateEvaluator = PredicateEvaluator(cardRegistry = null)

/**
 * Maskwood Nexus (KHM): "Creatures you control are every creature type. The same is true for
 * creature spells you control and creature cards you own that aren't on the battlefield."
 */
class MaskwoodNexusScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MaskwoodNexus, bear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun hasSubtype(driver: GameTestDriver, entityId: com.wingedsheep.sdk.model.EntityId, type: String): Boolean {
        val state = driver.state
        return predicateEvaluator.matchesCardPredicate(
            state, projector.project(state), entityId, CardPredicate.HasSubtype(Subtype(type))
        )
    }

    test("creatures you control are every creature type; an opponent's are not") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Maskwood Nexus")

        val myBear = driver.putCreatureOnBattlefield(me, "Test Bear")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Test Bear")

        val projected = projector.project(driver.state)
        projected.hasSubtype(myBear, "Bear") shouldBe true
        projected.hasSubtype(myBear, "Elf") shouldBe true
        projected.hasSubtype(myBear, "Squirrel") shouldBe true
        projected.hasSubtype(theirBear, "Elf") shouldBe false
    }

    test("creature cards you own outside the battlefield are every creature type; noncreatures and an opponent's are not") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Maskwood Nexus")

        val graveyardBear = driver.putCardInGraveyard(me, "Test Bear")
        val handBear = driver.putCardInHand(me, "Test Bear")
        val graveyardForest = driver.putCardInGraveyard(me, "Forest")
        val theirBear = driver.putCardInGraveyard(opponent, "Test Bear")

        hasSubtype(driver, graveyardBear, "Rat") shouldBe true
        hasSubtype(driver, graveyardBear, "Bear") shouldBe true
        hasSubtype(driver, handBear, "Wizard") shouldBe true
        hasSubtype(driver, graveyardForest, "Rat") shouldBe false
        hasSubtype(driver, theirBear, "Rat") shouldBe false
    }

    test("a creature spell you control is every creature type on the stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Maskwood Nexus")

        val spell = driver.putCardInHand(me, "Test Bear")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, spell)

        hasSubtype(driver, driver.state.stack.last(), "Dragon") shouldBe true
    }

    test("without the Nexus, a graveyard creature card keeps only its printed type") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val graveyardBear = driver.putCardInGraveyard(me, "Test Bear")

        hasSubtype(driver, graveyardBear, "Rat") shouldBe false
        hasSubtype(driver, graveyardBear, "Bear") shouldBe true
    }

    test("{3}, {T}: creates a 2/2 blue Shapeshifter with changeling") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val nexus = driver.putPermanentOnBattlefield(me, "Maskwood Nexus")
        driver.giveMana(me, Color.BLUE, 3)

        val ability = MaskwoodNexus.script.activatedAbilities.single()
        driver.submitSuccess(
            com.wingedsheep.engine.core.ActivateAbility(playerId = me, sourceId = nexus, abilityId = ability.id)
        )
        driver.bothPass()

        val token = driver.getCreatures(me).single { it != nexus }
        val projected = projector.project(driver.state)
        projected.getPower(token) shouldBe 2
        projected.getToughness(token) shouldBe 2
        projected.hasSubtype(token, "Shapeshifter") shouldBe true
        projected.hasColor(token, Color.BLUE) shouldBe true
    }
})
