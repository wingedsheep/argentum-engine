package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.ShadowsOverInnistradSet
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ulvenwald Mysteries (SOI) — {2}{G} Enchantment
 *
 * "Whenever a nontoken creature you control dies, investigate.
 *  Whenever you sacrifice a Clue, create a 1/1 white Human Soldier creature token."
 *
 * The two halves chain: the first makes Clues off your own dying creatures, the second pays off when
 * those Clues get cracked. The pieces are stock primitives, but the pairing is what has to work —
 * a leaves-the-battlefield trigger feeding [com.wingedsheep.sdk.dsl.Effects.Investigate], and a
 * per-object `PermanentsSacrificedEvent` filtered to Clues catching the Clue token's own
 * "{2}, Sacrifice this token: Draw a card."
 */
class UlvenwaldMysteriesScenarioTest : FunSpec({

    val clueSacAbilityId = PredefinedTokens.Clue.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShadowsOverInnistradSet.cards)
        driver.registerCard(PredefinedTokens.Clue)
        return driver
    }

    test("a nontoken creature you control dying investigates") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "Ulvenwald Mysteries")
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")

        driver.findPermanent(me, "Clue") shouldBe null

        // Kill my own creature — 3 damage to a 2/2.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass() // Bolt resolves, Goblin dies, dies-trigger goes on the stack
        driver.bothPass() // investigate resolves

        driver.findPermanent(me, "Clue") shouldNotBe null
    }

    test("sacrificing that Clue creates a 1/1 white Human Soldier") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "Ulvenwald Mysteries")
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Guide")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(goblin)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        val clue = driver.findPermanent(me, "Clue")!!
        driver.findPermanent(me, "Human Soldier Token") shouldBe null

        // Crack the Clue: "{2}, Sacrifice this token: Draw a card."
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = clue, abilityId = clueSacAbilityId))
        driver.bothPass() // draw resolves
        driver.bothPass() // the sacrifice-a-Clue payoff resolves

        val soldier = driver.findPermanent(me, "Human Soldier Token")
        soldier shouldNotBe null
        val projected = driver.state.projectedState
        projected.getPower(soldier!!) shouldBe 1
        projected.getToughness(soldier) shouldBe 1
        driver.state.projectedState.getSubtypes(soldier) shouldBe setOf("Human", "Soldier")
    }

    test("an opponent's creature dying does not investigate") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putPermanentOnBattlefield(me, "Ulvenwald Mysteries")
        val theirGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(theirGoblin)).isSuccess shouldBe true
        driver.bothPass()

        driver.getGraveyardCardNames(opponent).contains("Goblin Guide") shouldBe true
        driver.findPermanent(me, "Clue") shouldBe null
    }
})
