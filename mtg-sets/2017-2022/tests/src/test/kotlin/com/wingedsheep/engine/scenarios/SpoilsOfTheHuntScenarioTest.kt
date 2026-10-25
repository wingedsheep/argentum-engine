package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.SpoilsOfTheHunt
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Spoils of the Hunt (AFR #205) — {2}{G} Instant.
 *
 *   Target creature you control gets +1/+0 until end of turn for each mana from a Treasure that was
 *   spent to cast this spell. Then that creature deals damage equal to its power to target creature
 *   an opponent controls.
 *
 * The pump is cost-linked: it counts Treasure mana actually spent on the cast (an untapped Treasure
 * adds nothing), and the bite reads the creature's power after the pump.
 */
class SpoilsOfTheHuntScenarioTest : FunSpec({

    val treasureManaAbilityId = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SpoilsOfTheHunt))
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("without Treasure mana the creature bites for its unmodified power") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putPermanentOnBattlefield(me, "Treasure")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val spell = driver.putCardInHand(me, "Spoils of the Hunt")
        driver.giveMana(me, Color.GREEN, 3)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.bothPass()

        withClue("an unused Treasure adds no power") {
            driver.state.projectedState.getPower(bears) shouldBe 2
        }
        withClue("2 damage doesn't kill a 3/3") {
            driver.getGraveyardCardNames(opponent) shouldNotContain "Centaur Courser"
        }
    }

    test("each Treasure mana spent adds +1/+0 before the bite") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val treasureA = driver.putPermanentOnBattlefield(me, "Treasure")
        val treasureB = driver.putPermanentOnBattlefield(me, "Treasure")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        for (treasure in listOf(treasureA, treasureB)) {
            driver.submitSuccess(
                ActivateAbility(
                    playerId = me,
                    sourceId = treasure,
                    abilityId = treasureManaAbilityId,
                    manaColorChoice = Color.GREEN,
                )
            )
        }
        driver.giveMana(me, Color.GREEN, 1)

        val spell = driver.putCardInHand(me, "Spoils of the Hunt")
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        driver.bothPass()

        withClue("two Treasure mana → +2/+0") {
            driver.state.projectedState.getPower(bears) shouldBe 4
            driver.state.projectedState.getToughness(bears) shouldBe 2
        }
        withClue("the pumped 4 power kills the 3/3") {
            driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"
        }
    }
})
