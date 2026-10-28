package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whispering Madness
 * {2}{U}{B}
 * Sorcery
 * Each player discards their hand, then draws cards equal to the greatest number of cards a player
 * discarded this way.
 * Cipher
 *
 * The spell text is Windfall's shape: the greatest hand size is frozen before anyone discards, then
 * every hand is discarded, then everyone draws that many. Cipher is the keyword alone — the engine
 * runs the encoding step after the draws (the ruling: you draw before choosing the creature), and
 * each copy cast off the encoded creature's trigger repeats the wheel.
 */
val WhisperingMadness = card("Whispering Madness") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Each player discards their hand, then draws cards equal to the greatest number " +
        "of cards a player discarded this way.\n" +
        "Cipher (Then you may exile this spell card encoded on a creature you control. Whenever " +
        "that creature deals combat damage to a player, its controller may cast a copy of the " +
        "encoded card without paying its mana cost.)"

    keywords(Keyword.CIPHER)

    spell {
        effect = Effects.StoreNumber(
            "whisperingMadnessGreatestDiscard",
            DynamicAmounts.greatestAmongPlayers(DynamicAmounts.cardsInYourHand(), Player.Each)
        ) then
            Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
            Effects.ForEachPlayer(
                Player.Each,
                Effects.DrawCards(DynamicAmounts.storedNumber("whisperingMadnessGreatestDiscard"))
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64e4b0cc-e611-4a4b-8392-b37bfc3a77e1.jpg?1783940098"
        ruling("2013-04-15", "If a creature with an encoded card deals combat damage to more than one player simultaneously (perhaps because some of the combat damage was redirected), the triggered ability will trigger once for each player it deals combat damage to. Each ability will create a copy of the exiled card and allow you to cast it.")
        ruling("2013-04-15", "The exiled card with cipher grants a triggered ability to the creature it's encoded on. If that creature loses that ability and subsequently deals combat damage to a player, the triggered ability won't trigger. However, the exiled card will continue to be encoded on that creature.")
        ruling("2013-04-15", "If another player gains control of the creature, that player will control the triggered ability. That player will create a copy of the encoded card and may cast it.")
        ruling("2013-04-15", "The spell with cipher is encoded on the creature as part of that spell's resolution, just after the spell's other effects. That card goes directly from the stack to exile. It never goes to the graveyard.")
        ruling("2013-04-15", "You choose the creature as the spell resolves. The cipher ability doesn't target that creature, although the spell with cipher may target that creature (or a different creature) because of its other abilities.")
        ruling("2013-04-15", "If the spell with cipher doesn't resolve, none of its effects will happen, including cipher. The card will go to its owner's graveyard and won't be encoded on a creature.")
        ruling("2013-04-15", "If the creature leaves the battlefield, the exiled card will no longer be encoded on any creature. It will stay exiled.")
        ruling("2013-04-15", "The copy of the card with cipher is created in and cast from exile.")
        ruling("2013-04-15", "You cast the copy of the card with cipher during the resolution of the triggered ability. Ignore timing restrictions based on the card's type.")
        ruling("2013-04-15", "If you choose not to cast the copy, or you can't cast it (perhaps because there are no legal targets available), the copy will cease to exist the next time state-based actions are performed. You won't get a chance to cast the copy at a later time.")
        ruling("2013-01-24", "You'll draw cards before deciding which creature (if any) Whispering Madness will be encoded on.")
    }
}
