package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Break Asunder.
 *
 * Break Asunder: {2}{G}{G}
 * Sorcery
 * Destroy target artifact or enchantment.
 * Cycling {2}
 */
class BreakAsunderTest : FunSpec({

    val BreakAsunder = CardDefinition.sorcery(
        name = "Break Asunder",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        oracleText = "Destroy target artifact or enchantment.\nCycling {2}",
        script = CardScript.spell(
            effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true),
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
        )
    ).let { card ->
        card.copy(keywordAbilities = card.keywordAbilities + KeywordAbility.cycling("{2}"))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BreakAsunder))
        return driver
    }

    test("Break Asunder destroys target enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enchantment = driver.putPermanentOnBattlefield(opponent, "Test Enchantment")
        driver.findPermanent(opponent, "Test Enchantment") shouldNotBe null

        val breakAsunder = driver.putCardInHand(activePlayer, "Break Asunder")
        driver.giveMana(activePlayer, Color.GREEN, 4)

        val castResult = driver.castSpell(activePlayer, breakAsunder, listOf(enchantment))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Test Enchantment") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Test Enchantment"
    }

    test("Break Asunder destroys target artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putCreatureOnBattlefield(opponent, "Artifact Creature")
        driver.findPermanent(opponent, "Artifact Creature") shouldNotBe null

        val breakAsunder = driver.putCardInHand(activePlayer, "Break Asunder")
        driver.giveMana(activePlayer, Color.GREEN, 4)

        val castResult = driver.castSpell(activePlayer, breakAsunder, listOf(artifact))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Artifact Creature"
    }

    test("Break Asunder cannot target non-artifact non-enchantment creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val breakAsunder = driver.putCardInHand(activePlayer, "Break Asunder")
        driver.giveMana(activePlayer, Color.GREEN, 4)

        val castResult = driver.castSpell(activePlayer, breakAsunder, listOf(creature))
        castResult.isSuccess shouldBe false

        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("Break Asunder can be cycled to draw a card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val breakAsunder = driver.putCardInHand(activePlayer, "Break Asunder")
        val handSizeBefore = driver.getHandSize(activePlayer)
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val result = driver.submit(CycleCard(activePlayer, breakAsunder))
        result.isSuccess shouldBe true

        // Cycling discards the card and draws a new one, so hand size stays the same
        // (minus the cycled card, plus the drawn card)
        driver.getHandSize(activePlayer) shouldBe handSizeBefore
        driver.getGraveyardCardNames(activePlayer) shouldContain "Break Asunder"
    }
})
