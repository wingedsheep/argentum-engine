package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SoulFoundry
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Soul Foundry (MRD #246) — "Imprint — When this artifact enters, you may exile a creature card
 * from your hand. {X}, {T}: Create a token that's a copy of the exiled card. X is the mana value
 * of that card."
 *
 * Two things are worth pinning. The imprint is the Isochron Scepter shape — the card stays in the
 * linked-exile pile, so the Foundry keeps minting copies of it. And the activation cost is a
 * *defined* X (CR 107.3c): the player is never asked for a number, the ability is offered at the
 * imprinted card's mana value, and with no imprint at all it collapses to a legal but pointless
 * "{0}, {T}".
 */
class SoulFoundryScenarioTest : FunSpec({

    val foundryAbility = SoulFoundry.activatedAbilities.single().id
    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast the Foundry from hand so the imprint trigger actually fires, and return it. */
    fun GameTestDriver.castFoundry(): EntityId {
        val inHand = putCardInHand(player1, "Soul Foundry")
        giveColorlessMana(player1, 4)
        castSpell(player1, inHand).error shouldBe null
        // One pass resolves the artifact, the next resolves the imprint trigger it put on the stack.
        repeat(4) { if (state.pendingDecision == null) bothPass() }
        return findPermanent(player1, "Soul Foundry")!!
    }

    fun GameTestDriver.foundryAction(foundry: EntityId) =
        legalActions(player1).single {
            val a = it.action
            a is ActivateAbility && a.sourceId == foundry && a.abilityId == foundryAbility
        }

    test("imprints a creature card and mints a token copy for its mana value") {
        val d = driver()
        val bears = d.putCardInHand(d.player1, "Grizzly Bears") // {1}{G} — mana value 2
        // A second creature card, so the imprint is a real choice and the price below can only
        // have come from the card actually chosen.
        d.putCardInHand(d.player1, "Hill Giant") // {3}{R} — mana value 4
        val foundry = d.castFoundry()

        withClue("the imprint is a 'may', so it asks first") {
            d.submitYesNo(d.player1, true).error shouldBe null
        }
        d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null
        d.getExileCardNames(d.player1) shouldBe listOf("Grizzly Bears")

        withClue("the printed {X} is offered as the imprinted card's mana value, with no X picker") {
            val action = d.foundryAction(foundry)
            action.manaCostString shouldBe "{2}"
            action.hasXCost shouldBe false
        }

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, foundry, foundryAbility)).error shouldBe null
        withClue("no 'choose X' prompt — the card defined X") {
            d.state.pendingDecision shouldBe null
        }
        d.bothPass()

        val token = d.getCreatures(d.player1).single { d.getCardName(it) == "Grizzly Bears" }
        withClue("it is a token, not the exiled card moved onto the battlefield") {
            d.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
        }
        withClue("and a copy in every way — same P/T, and the card's own mana cost (2004-12-01 ruling)") {
            val projected = projector.project(d.state)
            projected.getPower(token) shouldBe 2
            projected.getToughness(token) shouldBe 2
            d.state.getEntity(token)!!.get<CardComponent>()!!.manaValue shouldBe 2
        }
        withClue("the imprinted card is still exiled — the Foundry copies, it doesn't spend") {
            d.getExileCardNames(d.player1) shouldBe listOf("Grizzly Bears")
        }
    }

    test("the price follows the imprinted card — a six-drop costs {6}") {
        val d = driver()
        val dragon = d.putCardInHand(d.player1, "Shivan Dragon") // {4}{R}{R} — mana value 6
        d.putCardInHand(d.player1, "Grizzly Bears")
        val foundry = d.castFoundry()

        d.submitYesNo(d.player1, true).error shouldBe null
        d.submitCardSelection(d.player1, listOf(dragon)).error shouldBe null

        withClue("five mana isn't enough for a Shivan Dragon imprint") {
            d.giveColorlessMana(d.player1, 5)
            d.foundryAction(foundry).let {
                it.manaCostString shouldBe "{6}"
                it.affordable shouldBe false
            }
        }
        withClue("the sixth covers it") {
            d.giveColorlessMana(d.player1, 1)
            d.foundryAction(foundry).affordable shouldBe true
        }

        d.submit(ActivateAbility(d.player1, foundry, foundryAbility)).error shouldBe null
        d.bothPass()
        d.getCreatures(d.player1).map { d.getCardName(it) } shouldBe listOf("Shivan Dragon")
    }

    test("a zero-mana-value imprint is free, and still mints the token") {
        val d = driver()
        val thopter = d.putCardInHand(d.player1, "Ornithopter") // {0} — mana value 0
        d.putCardInHand(d.player1, "Grizzly Bears")
        val foundry = d.castFoundry()

        d.submitYesNo(d.player1, true).error shouldBe null
        d.submitCardSelection(d.player1, listOf(thopter)).error shouldBe null

        withClue("X is 0 because the card costs nothing, not because nothing is imprinted") {
            d.foundryAction(foundry).manaCostString shouldBe "{0}"
        }
        d.submit(ActivateAbility(d.player1, foundry, foundryAbility)).error shouldBe null
        d.bothPass()

        d.getCreatures(d.player1).map { d.getCardName(it) } shouldBe listOf("Ornithopter")
    }

    test("declining the imprint leaves a legal but pointless {0}, {T}") {
        val d = driver()
        d.putCardInHand(d.player1, "Grizzly Bears")
        val foundry = d.castFoundry()

        d.submitYesNo(d.player1, false).error shouldBe null
        d.getExileCardNames(d.player1) shouldBe emptyList()

        withClue("an empty linked-exile pile defines X as 0") {
            val action = d.foundryAction(foundry)
            action.manaCostString shouldBe "{0}"
            action.affordable shouldBe true
        }

        d.submit(ActivateAbility(d.player1, foundry, foundryAbility)).error shouldBe null
        d.state.pendingDecision shouldBe null
        d.bothPass()

        withClue("nothing to copy, so no token") {
            d.getCreatures(d.player1) shouldBe emptyList()
        }
    }

    test("a hand with no creature card imprints nothing") {
        val d = driver()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.castFoundry()

        d.submitYesNo(d.player1, true).error shouldBe null

        withClue("there were no legal candidates, so nothing was exiled") {
            d.getExileCardNames(d.player1) shouldBe emptyList()
            d.findCardInHand(d.player1, "Lightning Bolt") shouldBe bolt
        }
    }
})
