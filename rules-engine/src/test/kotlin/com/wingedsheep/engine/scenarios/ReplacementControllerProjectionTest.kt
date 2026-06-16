package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.BloodletterOfAclazotz
import com.wingedsheep.mtg.sets.definitions.ons.cards.BlatantThievery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import io.kotest.core.spec.style.FunSpec

/**
 * Damage/life-loss replacement effects are hosted on battlefield permanents, and their
 * "you" / "an opponent" filters are resolved relative to the *host permanent's controller*.
 * When a control-changing effect steals such a permanent, those filters must follow the new
 * controller (the projected controller, post Layer-2), not the printed one.
 *
 * `DamageUtils` resolves the host's controller for every replacement scan; these tests steal
 * a replacement-effect permanent and assert the filter retargets to the thief. They cover the
 * two distinct comparison shapes:
 *  - `Player.You` / `Player.EachOpponent` against the losing player (`ModifyLifeLoss` — Bloodletter)
 *  - `RecipientFilter.You` against the damaged player (`PreventDamage`)
 */
class ReplacementControllerProjectionTest : FunSpec({

    // 0/4 wall whose only ability is "prevent all damage that would be dealt to you" —
    // a `PreventDamage` keyed to `RecipientFilter.You`, i.e. to the host's controller.
    val aegisSentinel = CardDefinition.creature(
        name = "Aegis Sentinel",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Wall")),
        power = 0,
        toughness = 4,
        oracleText = "Prevent all damage that would be dealt to you.",
        script = CardScript.withReplacementEffects(
            PreventDamage(appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.You)),
        ),
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BloodletterOfAclazotz, BlatantThievery, aegisSentinel))
        return driver
    }

    test("stealing Bloodletter of Aclazotz doubles the THIEF's opponent's life loss, not the original owner's") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Island" to 20), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent owns Bloodletter (its "if an opponent would lose life during your turn"
        // currently points at YOU). Steal it onto your side.
        val bloodletter = driver.putCreatureOnBattlefield(opponent, "Bloodletter of Aclazotz")

        driver.giveMana(you, Color.BLUE, 7)
        val thievery = driver.putCardInHand(you, "Blatant Thievery")
        driver.castSpellWithTargets(you, thievery, listOf(ChosenTarget.Permanent(bloodletter)))
        driver.bothPass()

        // Now YOU control Bloodletter, it's your turn, and `opponent` is your opponent.
        // 3 damage to them must be doubled to 6 (20 -> 14). Under the projection bug the
        // host's printed controller (opponent) is used, so the filter never matches and they
        // would take only 3 (20 -> 17).
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.assertLifeTotal(opponent, 14)
    }

    test("stealing a 'prevent damage to you' permanent protects the THIEF, not the original owner") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Island" to 20), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sentinel = driver.putCreatureOnBattlefield(opponent, "Aegis Sentinel")

        driver.giveMana(you, Color.BLUE, 7)
        val thievery = driver.putCardInHand(you, "Blatant Thievery")
        driver.castSpellWithTargets(you, thievery, listOf(ChosenTarget.Permanent(sentinel)))
        driver.bothPass()

        // Now YOU control the Sentinel, so "prevent all damage to you" protects YOU.
        // 3 damage to yourself is prevented (life stays 20). Under the projection bug it would
        // still protect the original owner, so your 3 would resolve (20 -> 17).
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.bothPass()

        driver.assertLifeTotal(you, 20)
    }
})
