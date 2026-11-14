package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.TheGreatAurora
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Great Aurora — {6}{G}{G}{G} Sorcery (Magic Origins #179)
 *
 * "Each player shuffles all cards from their hand and all permanents they own into their library,
 *  then draws that many cards. Each player may put any number of land cards from their hand onto
 *  the battlefield. Exile The Great Aurora."
 *
 * Every test empties both hands and libraries first, so each player's library holds exactly what
 * they shuffled in (plus any card the test planted) and "draws that many" has a fixed outcome.
 */
class TheGreatAuroraScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TheGreatAurora)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        var s = d.state
        for (p in s.turnOrder) {
            for (zone in listOf(Zone.LIBRARY, Zone.HAND)) {
                for (id in s.getZone(ZoneKey(p, zone))) {
                    s = s.removeFromZone(ZoneKey(p, zone), id).addToZone(ZoneKey(p, Zone.EXILE), id)
                }
            }
        }
        d.replaceState(s)
        return d
    }

    fun GameTestDriver.castAurora() {
        val aurora = putCardInHand(player1, "The Great Aurora")
        giveMana(player1, Color.GREEN, 3)
        giveColorlessMana(player1, 6)
        castSpell(player1, aurora).error shouldBe null
        bothPass()
    }

    test("each player draws for their own hand and permanents, tokens included, then puts lands down together") {
        val d = driver()
        val mountain = d.putLandOnBattlefield(d.player1, "Mountain")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val forest = d.putCardInHand(d.player1, "Forest")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")

        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val token = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.addComponent(token, TokenComponent)
        val theirMountain = d.putCardInHand(d.player2, "Mountain")
        val theirBolt = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")

        d.castAurora()

        withClue("you shuffled in four cards and drew all four back") {
            d.getHand(d.player1) shouldContainExactlyInAnyOrder listOf(mountain, courser, forest, bolt)
            d.state.getLibrary(d.player1).shouldBeEmpty()
        }
        withClue("the opponent's token counted: three drawn, past the planted Bolt, never the token") {
            d.getHand(d.player2) shouldContainExactlyInAnyOrder listOf(lions, theirMountain, theirBolt)
        }

        val yours = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("you choose first, among the land cards in your hand only") {
            yours.playerId shouldBe d.player1
            yours.options shouldContainExactlyInAnyOrder listOf(mountain, forest)
            yours.minSelections shouldBe 0
        }
        d.submitCardSelection(d.player1, listOf(forest)).error shouldBe null
        withClue("nothing enters until every player has chosen") {
            d.state.getBattlefield() shouldNotContain forest
        }

        val theirs = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        theirs.playerId shouldBe d.player2
        theirs.options shouldContainExactly listOf(theirMountain)
        d.submitCardSelection(d.player2, listOf(theirMountain)).error shouldBe null

        d.pendingDecision shouldBe null
        d.state.stack.shouldBeEmpty()
        d.getPermanents(d.player1) shouldContainExactly listOf(forest)
        d.getPermanents(d.player2) shouldContainExactly listOf(theirMountain)
        d.getHand(d.player1) shouldContainExactlyInAnyOrder listOf(mountain, courser, bolt)
        d.getHand(d.player2) shouldContainExactlyInAnyOrder listOf(lions, theirBolt)
        withClue("the token ceased to exist") {
            d.state.getBattlefield() shouldNotContain token
            d.state.getLibrary(d.player2).shouldBeEmpty()
        }
        withClue("The Great Aurora exiles itself") {
            d.getExileCardNames(d.player1) shouldContain "The Great Aurora"
            d.getGraveyardCardNames(d.player1) shouldNotContain "The Great Aurora"
        }
    }

    test("a permanent you control but don't own goes to its owner's library and its owner draws for it") {
        val d = driver()
        val mountain = d.putLandOnBattlefield(d.player1, "Mountain")
        val stolen = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.replaceState(d.state.updateEntity(stolen) { it.with(ControllerComponent(d.player1)) })

        d.castAurora()

        var guard = 0
        while (d.pendingDecision is SelectCardsDecision && guard++ < 4) {
            val decision = d.pendingDecision as SelectCardsDecision
            d.submitCardSelection(decision.playerId, emptyList()).error shouldBe null
        }

        d.state.stack.shouldBeEmpty()
        withClue("you drew one card, for your Mountain — not one for the stolen Lions") {
            d.getHand(d.player1) shouldContainExactly listOf(mountain)
        }
        withClue("the Lions went home and their owner drew it") {
            d.getHand(d.player2) shouldContainExactly listOf(stolen)
        }
        d.state.getLibrary(d.player1).shouldBeEmpty()
        d.state.getLibrary(d.player2).shouldBeEmpty()
        d.state.getBattlefield().shouldBeEmpty()
    }
})
