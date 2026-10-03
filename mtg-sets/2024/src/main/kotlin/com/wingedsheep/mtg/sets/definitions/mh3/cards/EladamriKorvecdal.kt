package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Eladamri, Korvecdal
 * {1}{G}{G}
 * Legendary Creature — Elf Warrior
 * 3/3
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 * {G}, {T}, Tap two untapped creatures you control: Reveal a card from your hand or the top card
 * of your library. If you reveal a creature card this way, put it onto the battlefield. Activate
 * only during your turn.
 *
 * The "a card from your hand or the top card of your library" choice is made at resolution as a
 * single up-to-one pick from the hand: choosing a hand card reveals that card, and choosing none
 * reveals the top card of the library instead (an empty hand goes straight to the top card).
 */
val EladamriKorvecdal = card("Eladamri, Korvecdal") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast creature spells from the top of your library.\n" +
        "{G}, {T}, Tap two untapped creatures you control: Reveal a card from your hand or the top card " +
        "of your library. If you reveal a creature card this way, put it onto the battlefield. " +
        "Activate only during your turn."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Creature
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Tap,
            Costs.TapPermanents(2, GameObjectFilter.Creature)
        )
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(zone = Zone.HAND, player = Player.You))
            val fromHand = chooseUpTo(
                1,
                from = hand,
                prompt = "Reveal a card from your hand, or choose none to reveal the top card of your library"
            )
            ifNotEmpty(fromHand) {
                reveal(fromHand, fromZone = Zone.HAND)
                move(fromHand, CardDestination.ToZone(Zone.BATTLEFIELD), filter = GameObjectFilter.Creature)
            } orElse {
                val top = gather(CardSource.TopOfLibrary(1), revealed = true)
                move(top, CardDestination.ToZone(Zone.BATTLEFIELD), filter = GameObjectFilter.Creature)
            }
        }
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        description = "{G}, {T}, Tap two untapped creatures you control: Reveal a card from your hand or " +
            "the top card of your library. If it's a creature card, put it onto the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "149"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfdabdda-bd46-4ea2-8b37-5d7f6cec6aff.jpg?1783911264"
        ruling(
            "2024-06-07",
            "Because you never \"cast\" a land card, Eladamri, Korvecdal doesn't allow you to play a land " +
                "creature (such as Dryad Arbor) from the top of your library."
        )
    }
}
