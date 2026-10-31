package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Parcelbeast
 * {2}{G}{U}
 * Creature — Elemental Beast
 * 2/4
 * Mutate {G}{U}
 * {1}, {T}: Look at the top card of your library. If it's a land card, you may put it onto the
 * battlefield. If you don't put the card onto the battlefield, put it into your hand.
 *
 * The look is Skyclave Invader's pipeline: split the top card into land / nonland, offer up to one
 * land onto the battlefield (untapped), and everything not put onto the battlefield goes to hand.
 */
val Parcelbeast = card("Parcelbeast") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elemental Beast"
    power = 2
    toughness = 4
    oracleText = "Mutate {G}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "{1}, {T}: Look at the top card of your library. If it's a land card, you may put it onto the battlefield. " +
        "If you don't put the card onto the battlefield, put it into your hand."

    keywordAbility(KeywordAbility.mutate("{G}{U}"))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (landCards, nonLandCards) = filterSplit(looked, GameObjectFilter.Land)
            val (toBattlefield, landsToHand) = chooseUpToSplit(
                1,
                from = landCards,
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            toHand(landsToHand)
            toHand(nonLandCards)
        }
        description = "Look at the top card of your library. If it's a land card, you may put it onto the " +
            "battlefield. If you don't put the card onto the battlefield, put it into your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/610bb98c-d66a-44cc-92e2-a80d700b59e4.jpg?1783931019"
    }
}
