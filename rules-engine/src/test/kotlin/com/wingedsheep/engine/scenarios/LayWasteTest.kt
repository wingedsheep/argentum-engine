package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Lay Waste.
 *
 * Lay Waste: {3}{R}
 * Sorcery
 * Destroy target land.
 * Cycling {2}
 */
class LayWasteTest : FunSpec({

    val LayWaste = CardDefinition.sorcery(
        name = "Lay Waste",
        manaCost = ManaCost.parse("{3}{R}"),
        oracleText = "Destroy target land.\nCycling {2}",
        script = CardScript.spell(
            effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true),
            TargetPermanent(filter = TargetFilter.Land)
        ),
        metadata = com.wingedsheep.sdk.model.ScryfallMetadata()
    ).let { card ->
        // Add cycling keyword ability
        card.copy(keywordAbilities = card.keywordAbilities + KeywordAbility.cycling("{2}"))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LayWaste))
        return driver
    }

    test("Lay Waste destroys target land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a land on opponent's battlefield
        val forest = driver.putLandOnBattlefield(opponent, "Forest")
        driver.findPermanent(opponent, "Forest") shouldNotBe null

        // Cast Lay Waste targeting the land
        val layWaste = driver.putCardInHand(activePlayer, "Lay Waste")
        driver.giveMana(activePlayer, Color.RED, 4)

        val castResult = driver.castSpell(activePlayer, layWaste, listOf(forest))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        // The land should be destroyed
        driver.findPermanent(opponent, "Forest") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Forest"
    }

    test("Lay Waste cannot target creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val layWaste = driver.putCardInHand(activePlayer, "Lay Waste")
        driver.giveMana(activePlayer, Color.RED, 4)

        val castResult = driver.castSpell(activePlayer, layWaste, listOf(creature))
        castResult.isSuccess shouldBe false

        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
