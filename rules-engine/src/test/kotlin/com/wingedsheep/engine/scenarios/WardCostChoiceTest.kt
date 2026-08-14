package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine tests for `WardCost.Choice` — the OR disjunction, "Ward—Discard a card or pay {2}"
 * (Titania, Rugged Rumbler), CR 702.21a.
 *
 * The rules this pins down:
 * - the payer is asked *which* option first, then pays it through that cost's own ordinary flow;
 * - only options the payer can actually pay are offered, so the picker can never advertise a leg
 *   that would then fail (the discard leg disappears when nothing in hand matches its filter);
 * - a trailing decline option counters the spell, and so does declining inside the chosen leg;
 * - when no option is payable, the spell is countered with no prompt at all;
 * - it works identically when granted by a static ability (`GrantWard`).
 *
 * Note on hands: `initMirrorMatch` deals a real opening hand, so "nothing to discard" is set up
 * with a *filtered* discard leg no card in that hand matches, not by emptying the hand.
 */
class WardCostChoiceTest : FunSpec({

    val choiceWardedBear = card("Choice-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(KeywordAbility.wardDiscardOrPay("{2}"))
    }

    // Both legs need a resource the payer can lack, so "no option payable" is reachable: a hand of
    // basic lands matches no creature-card discard, and the payer controls no creature.
    val grimWardedBear = card("Grim-Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.WARD)
        keywordAbility(
            KeywordAbility.wardChoice(
                WardCost.Discard(filter = GameObjectFilter.Creature),
                WardCost.Sacrifice(GameObjectFilter.Creature),
            )
        )
    }

    val choiceWardEmitter = card("Choice Ward Emitter") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantWard(
                cost = WardCost.Choice(listOf(WardCost.Discard(), WardCost.Mana("{2}"))),
                filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
            )
        }
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(choiceWardedBear, grimWardedBear, choiceWardEmitter, plainBear)
        )
        return driver
    }

    test("both legs are offered when both are payable, and the discard leg discards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Choice-Warded Bear")

        // {R} for the bolt, {2} spare so the pay leg is affordable too.
        driver.giveMana(activePlayer, Color.RED, 3)
        val spare = driver.putCardInHand(activePlayer, "Forest")
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe activePlayer
        withClue("both legs plus a decline, each labelled with its own verb") {
            decision.options shouldContainExactly listOf("Discard a card", "Pay {2}", "Counter spell")
        }

        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 0))

        // The discard leg runs the ordinary ward-discard flow: confirm, then pick the card.
        driver.pendingDecision.shouldNotBeNull()
        driver.submitYesNo(activePlayer, true)
        if (driver.pendingDecision is SelectCardsDecision) {
            driver.submitCardSelection(activePlayer, listOf(spare))
        }
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.getGraveyardCardNames(activePlayer) shouldContain "Forest"
        driver.getHandSize(activePlayer) shouldBe (handAfterCast - 1)
        withClue("paying lets the bolt through") {
            driver.findPermanent(opponent, "Choice-Warded Bear") shouldBe null
        }
    }

    test("picking the mana leg pays mana and lets the spell through") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Choice-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 3)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 1))

        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(activePlayer, true)
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        withClue("nothing was discarded — the mana leg was taken") {
            driver.getHandSize(activePlayer) shouldBe handAfterCast
        }
        driver.findPermanent(opponent, "Choice-Warded Bear") shouldBe null
    }

    test("an unpayable leg is not offered at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grim-Warded Bear")

        // The caster controls a creature (the sacrifice leg is payable) but holds only Mountains,
        // so the creature-card discard leg is not.
        driver.putCreatureOnBattlefield(activePlayer, "Plain Bear")
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldContainExactly listOf("Sacrifice a creature", "Counter spell")
    }

    test("choosing the trailing decline counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Choice-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 3)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, decision.options.size - 1))
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        withClue("declining pays nothing and counters") {
            driver.getHandSize(activePlayer) shouldBe handAfterCast
            driver.findPermanent(opponent, "Choice-Warded Bear") shouldNotBe null
        }
    }

    test("declining inside the chosen leg still counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Choice-Warded Bear")

        driver.giveMana(activePlayer, Color.RED, 3)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val handAfterCast = driver.getHandSize(activePlayer)
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 0))
        driver.submitYesNo(activePlayer, false)
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.getHandSize(activePlayer) shouldBe handAfterCast
        driver.findPermanent(opponent, "Choice-Warded Bear") shouldNotBe null
    }

    test("no payable option counters the spell with no prompt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grim-Warded Bear")

        // Only Mountains in hand and no creatures on board — neither leg is payable.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Grim-Warded Bear") shouldNotBe null
    }

    test("a statically granted disjunctive ward prompts the same way") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Choice Ward Emitter")
        val bear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        driver.giveMana(activePlayer, Color.RED, 3)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    test("the printed wording renders as oracle text") {
        KeywordAbility.wardDiscardOrPay("{2}").description shouldBe "Ward—Discard a card or pay {2}"
        KeywordAbility.wardChoice(
            WardCost.Life(2),
            WardCost.Sacrifice(GameObjectFilter.Creature),
        ).description shouldBe "Ward—Pay 2 life or sacrifice a creature"
    }

    test("the trigger's own effect and a static grant render the disjunction too") {
        // Three renderings share the WardCost taxonomy: the keyword line above, the third-person
        // "Counter it unless its controller ~" on the trigger's effect (which joins *conjugated*
        // phrases, not clauses), and the granted-ward line.
        WardCounterEffect(
            WardCost.Choice(listOf(WardCost.Discard(), WardCost.Mana("{2}")))
        ).description shouldBe "Counter it unless its controller discards a card or pays {2}"
        GrantWard(
            cost = WardCost.Choice(listOf(WardCost.Discard(), WardCost.Mana("{2}"))),
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        ).description shouldContain "have \"Ward—Discard a card or pay {2}.\""
    }
})
