package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unexpected Results — Gatecrash #203
 * {2}{G}{U} Sorcery
 *
 * Shuffle, then reveal the top card. A nonland card may be cast from the library during resolution
 * without paying its mana cost (declined or uncastable, it stays on top). A land may be put onto the
 * battlefield; only if it is does the spell return itself to its owner's hand — straight from the
 * stack (`ReturnToHand(Self)`, as View from Above), so the resolver finds it gone and skips the
 * graveyard. The land choice is a `chooseUpTo(1)` so the bounce is gated on the land actually moving.
 */
val UnexpectedResults = card("Unexpected Results") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Shuffle your library, then reveal the top card. If it's a nonland card, you may cast it " +
        "without paying its mana cost. If it's a land card, you may put it onto the battlefield and return " +
        "Unexpected Results to its owner's hand."

    spell {
        effect = Effects.ShuffleLibrary() then Effects.Pipeline(
            descriptionOverride = "Reveal the top card of your library. If it's a nonland card, you may cast it " +
                "without paying its mana cost. If it's a land card, you may put it onto the battlefield and " +
                "return Unexpected Results to its owner's hand."
        ) {
            val top = gather(CardSource.TopOfLibrary(1), revealed = true)
            val land = chooseUpTo(
                1,
                filter(top, GameObjectFilter.Land),
                prompt = "You may put the revealed land onto the battlefield and return Unexpected Results to your hand",
                selectedLabel = "Put onto the battlefield",
            )
            ifNotEmpty(land) {
                move(land, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                run(Effects.ReturnToHand(EffectTarget.Self))
            }
            val spell = chooseSpell(
                from = filter(top, GameObjectFilter.Nonland),
                prompt = "You may cast the revealed card without paying its mana cost",
                selectedLabel = "Cast without paying",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spell))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65e10a22-6070-48d7-99ab-45f770f16fd1.jpg?1783940098"
        ruling("2013-01-24", "If you reveal a nonland card, you may cast it during the resolution of Unexpected Results. Ignore timing restrictions based on the card's type. Other timing restrictions, such as \"Cast [this card] only during combat,\" must be followed.")
        ruling("2013-01-24", "If you can't cast the card (perhaps because there are no legal targets), or if you choose not to, the card will remain on top of the library.")
        ruling("2013-01-24", "If you cast a spell \"without paying its mana cost,\" you can't pay alternative costs such as overload costs. You can pay additional costs such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2013-01-24", "If the card has {X} in its mana cost, you must choose 0 as its value.")
        ruling("2013-01-24", "If you reveal a land card, Unexpected Results will be returned to your hand only if you put that land card onto the battlefield. If you don't, Unexpected Results will be put into its owner's graveyard.")
        ruling("2013-01-24", "If you reveal a land card and put that card onto the battlefield, Unexpected Results will be put into its owner's hand directly from the stack. It won't be put into any graveyard.")
    }
}
