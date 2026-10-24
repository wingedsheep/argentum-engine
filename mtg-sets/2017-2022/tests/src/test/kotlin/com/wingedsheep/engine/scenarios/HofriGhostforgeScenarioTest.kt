package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Hofri Ghostforge (STX #192) — {3}{R}{W} 4/5 Legendary Creature — Dwarf Cleric.
 *
 * "Spirits you control get +1/+1 and have trample and haste. Whenever another nontoken creature
 *  you control dies, exile it. If you do, create a token that's a copy of that creature, except
 *  it's a Spirit in addition to its other types and it has 'When this token leaves the
 *  battlefield, return the exiled card to its owner's graveyard.'"
 *
 * Pins the link between the exiled card and the Spirit token(s) made from it: only that card
 * returns, and with a token doubler every token is linked to it (per the card's rulings).
 */
class HofriGhostforgeScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hofri Ghostforge")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.killBears() {
        castSpell(1, "Shock", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
    }

    init {
        test("a dying nontoken creature is exiled and returns as a pumped Spirit token copy") {
            val game = base().build()

            game.killBears()

            withClue("the dead card was exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.findCardsInGraveyard(1, "Grizzly Bears").shouldBeEmpty()
            }
            val token = game.findPermanent("Grizzly Bears")!!
            val projected = game.state.projectedState
            game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
            withClue("a Spirit in addition to its other types") {
                projected.hasSubtype(token, "Spirit") shouldBe true
                projected.hasSubtype(token, "Bear") shouldBe true
            }
            withClue("Hofri's lord bonus applies to the Spirit token") {
                projected.getPower(token) shouldBe 3
                projected.getToughness(token) shouldBe 3
                projected.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(token, Keyword.HASTE) shouldBe true
            }
        }

        test("when the token leaves the battlefield, the exiled card goes to its owner's graveyard") {
            val game = base()
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 1)
                .build()

            game.killBears()
            game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInExile(1, "Grizzly Bears") shouldBe false
            game.findCardsInGraveyard(1, "Grizzly Bears") shouldHaveSize 1
        }

        test("the token dying does not retrigger Hofri; only the linked card returns") {
            val game = base()
                .withCardInExile(1, "Grizzly Bears")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .build()

            game.killBears()
            game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            withClue("the Hofri-exiled Bears returned; the unrelated exiled Bears stayed") {
                game.findCardsInGraveyard(1, "Grizzly Bears") shouldHaveSize 1
                game.isInExile(1, "Grizzly Bears") shouldBe true
            }
        }

        test("with a token doubler every Spirit token is linked to the exiled card") {
            val game = base()
                .withCardOnBattlefield(1, "Parallel Lives")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Unsummon")
                .build()

            game.killBears()
            val tokens = game.findPermanents("Grizzly Bears")
            tokens shouldHaveSize 2

            game.castSpell(1, "Unsummon", tokens[1]).error shouldBe null
            game.resolveStack()
            withClue("either token leaving returns the card") {
                game.findCardsInGraveyard(1, "Grizzly Bears") shouldHaveSize 1
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }

            game.castSpell(1, "Unsummon", tokens[0]).error shouldBe null
            game.resolveStack()
            game.findPermanents("Grizzly Bears").shouldBeEmpty()
            game.findCardsInGraveyard(1, "Grizzly Bears") shouldHaveSize 1
        }
    }
}
