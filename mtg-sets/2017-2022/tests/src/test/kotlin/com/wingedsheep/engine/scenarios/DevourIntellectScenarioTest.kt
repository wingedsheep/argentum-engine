package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.DevourIntellect
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Devour Intellect (AFR #97) — {B} Sorcery.
 *
 *   Target opponent discards a card. If mana from a Treasure was spent to cast this spell, instead
 *   that player reveals their hand, you choose a nonland card from it, then that player discards
 *   that card.
 *
 * The branch reads the resolving spell's own mana provenance: controlling an untapped Treasure is
 * not spending its mana, and the Treasure is already sacrificed by the time the spell resolves.
 */
class DevourIntellectScenarioTest : FunSpec({

    val treasureManaAbilityId = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DevourIntellect))
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("without Treasure mana the opponent chooses which card to discard") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        // A Treasure on the battlefield that is never tapped must not flip the branch.
        driver.putPermanentOnBattlefield(me, "Treasure")
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Devour Intellect")
        driver.giveMana(me, Color.BLACK, 1)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the plain mode is the opponent's choice") {
            decision.playerId shouldBe opponent
        }
        driver.submitCardSelection(opponent, listOf(bears)).outcome shouldBe Outcome.Done
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
    }

    test("with Treasure mana the caster chooses a nonland card from the revealed hand") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val treasure = driver.putPermanentOnBattlefield(me, "Treasure")
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        val swamp = driver.putCardInHand(opponent, "Swamp")

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = treasure,
                abilityId = treasureManaAbilityId,
                manaColorChoice = Color.BLACK,
            )
        )

        val spell = driver.putCardInHand(me, "Devour Intellect")
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("Treasure mana was spent → the caster picks") {
            decision.playerId shouldBe me
        }
        withClue("a land in the revealed hand is not a legal pick") {
            decision.nonSelectableOptions shouldContain swamp
        }
        driver.submitCardSelection(me, listOf(bears)).outcome shouldBe Outcome.Done
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.getGraveyardCardNames(opponent) shouldNotContain "Swamp"
    }
})
