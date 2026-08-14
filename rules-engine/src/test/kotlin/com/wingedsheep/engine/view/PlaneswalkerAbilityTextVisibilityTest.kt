package com.wingedsheep.engine.view

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * [ClientStateTransformer] hands the client one [ClientPlaneswalkerAbility] per loyalty ability so
 * the card's full ability menu can be rendered (including the ones that aren't currently legal).
 * The text comes from the matching oracle-text line.
 *
 * A loyalty *cost* is not a unique key: Garruk Relentless has two 0-cost abilities, Jaya Ballard two
 * +1s, and so do Sorin, Imperious Bloodlord, Jace Reawakened, Arlinn Kord, and Chandra, Dressed to
 * Kill. Keying one oracle line per cost made every ability sharing a cost report the *first* line's
 * text, so those cards showed the same ability twice.
 */
class PlaneswalkerAbilityTextVisibilityTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun abilities(d: GameTestDriver, permanent: EntityId, viewer: EntityId) =
        ClientStateTransformer(cardRegistry = d.cardRegistry)
            .transform(d.state, viewingPlayerId = viewer)
            .cards[permanent]
            .shouldNotBeNull()
            .planeswalkerAbilities
            .shouldNotBeNull()

    test("Garruk Relentless's two 0-cost abilities each get their own oracle line") {
        val d = driver()
        val player = d.activePlayer!!
        val garruk = d.putPermanentOnBattlefield(player, "Garruk Relentless")

        val pwAbilities = abilities(d, garruk, player)

        pwAbilities.map { it.loyaltyChange } shouldBe listOf(0, 0)
        pwAbilities.map { it.description } shouldBe listOf(
            "Garruk deals 3 damage to target creature. That creature deals damage equal to its power to him",
            "Create a 2/2 green Wolf creature token",
        )
        pwAbilities.map { it.abilityId }.toSet().size shouldBe 2
    }

    test("Jaya Ballard's two +1 abilities are not collapsed onto the first one's text") {
        val d = driver()
        val player = d.activePlayer!!
        val jaya = d.putPermanentOnBattlefield(player, "Jaya Ballard")

        val pwAbilities = abilities(d, jaya, player)

        pwAbilities.map { it.loyaltyChange } shouldBe listOf(1, 1, -8)
        pwAbilities[0].description shouldBe
            "Add {R}{R}{R}. Spend this mana only to cast instant or sorcery spells"
        pwAbilities[1].description shouldBe "Discard up to three cards, then draw that many cards"
    }

    test("a planeswalker with distinct loyalty costs still maps each ability to its own line") {
        val d = driver()
        val player = d.activePlayer!!
        val liliana = d.putPermanentOnBattlefield(player, "Liliana of the Veil")

        val pwAbilities = abilities(d, liliana, player)

        pwAbilities.map { it.loyaltyChange } shouldBe listOf(1, -2, -6)
        pwAbilities.map { it.description } shouldBe listOf(
            "Each player discards a card",
            "Target player sacrifices a creature",
            "Separate all permanents target player controls into two piles. That player sacrifices " +
                "all permanents in the pile of their choice",
        )
    }
})
