package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.LithoformBlight
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Lithoform Blight — {1}{B} Aura: Enchant land. When this Aura enters, draw a card. Enchanted land
 * loses all land types and abilities and has "{T}: Add {C}" and "{T}, Pay 1 life: Add one mana of
 * any color."
 */
class LithoformBlightScenarioTest : FunSpec({

    val (colorlessId, anyColorId) = LithoformBlight.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().map { it.ability.id }

    // A nonbasic land with only non-basic land types, plus a non-land subtype that must survive.
    val desertGate = CardDefinition(
        name = "Test Desert Gate",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND, CardType.ARTIFACT),
            subtypes = setOf(Subtype("Desert"), Subtype("Gate"), Subtype("Equipment")),
        ),
        script = CardScript(),
    )

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LithoformBlight, desertGate))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("enchanting a basic land draws a card and leaves only the two granted mana abilities") {
        val driver = newGame()
        val player = driver.activePlayer!!
        val swamp = driver.putLandOnBattlefield(player, "Swamp")
        val aura = driver.putCardInHand(player, "Lithoform Blight")
        val handBefore = driver.getHandSize(player)
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, aura, listOf(swamp))
        driver.bothPass() // Aura resolves
        driver.bothPass() // draw trigger resolves

        driver.getHandSize(player) shouldBe handBefore // -1 Aura, +1 draw

        val projected = driver.state.projectedState
        projected.hasSubtype(swamp, "Swamp") shouldBe false
        projected.hasType(swamp, "LAND") shouldBe true
        projected.hasType(swamp, "BASIC") shouldBe true

        val actions = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.FULL)
        actions.mapNotNull { (it.action as? ActivateAbility)?.takeIf { a -> a.sourceId == swamp }?.abilityId }
            .distinct() shouldContainExactlyInAnyOrder listOf(colorlessId, anyColorId)

        driver.submitSuccess(
            ActivateAbility(playerId = player, sourceId = swamp, abilityId = anyColorId, manaColorChoice = Color.GREEN)
        )
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1
        driver.state.getEntity(player)?.get<LifeTotalComponent>()?.life shouldBe 19
    }

    test("strips non-basic land types but keeps other card types and non-land subtypes") {
        val driver = newGame()
        val player = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(player, "Test Desert Gate")
        val aura = driver.putCardInHand(player, "Lithoform Blight")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, aura, listOf(land))
        driver.bothPass()
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getSubtypes(land) shouldBe setOf("Equipment")
        projected.hasType(land, "ARTIFACT") shouldBe true
        projected.hasType(land, "LAND") shouldBe true

        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = land, abilityId = colorlessId))
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.colorless shouldBe 1
    }
})
