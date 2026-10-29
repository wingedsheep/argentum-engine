package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlOpponentsLibrarySearches

/**
 * Opposition Agent
 * {2}{B}
 * Creature — Human Rogue
 * 3/2
 * Flash
 * You control your opponents while they're searching their libraries.
 * While an opponent is searching their library, they exile each card they find. You may play those
 * cards for as long as they remain exiled, and you may spend mana as though it were mana of any
 * color to cast them.
 */
val OppositionAgent = card("Opposition Agent") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "You control your opponents while they're searching their libraries.\n" +
        "While an opponent is searching their library, they exile each card they find. You may play those " +
        "cards for as long as they remain exiled, and you may spend mana as though it were mana of any color to cast them."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ControlOpponentsLibrarySearches
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/086f97e9-8b62-44f3-b467-149c2ac5ca78.jpg?1783928831"
        ruling("2020-11-10", "You must follow the normal timing permissions and restrictions for each exiled card. If one is a land, you can't play it unless you have land plays available.")
        ruling("2020-11-10", "While controlling an opponent, you make all choices and decisions for that player. However, because the control effect is limited to while they're searching their libraries, it's unlikely the player will be allowed to make any decisions other than what to find with the search.")
        ruling("2020-11-10", "You can't have the player find cards in their library that aren't asked for by the search instruction. For example, if the opponent is searching their library for a basic land card, you can't have them find a different card, such as a creature card.")
        ruling("2020-11-10", "If the search instruction states a quality of the card to find, such as a color or card type, you may have the opponent not find any cards. If the search is simply for a number of cards without stating a quality, you must have them find that many cards (or as many as possible, if their library doesn't contain that many cards).")
        ruling("2020-11-10", "The cards found in the search will be exiled rather than be put wherever the spell or ability tells the opponent to put them after finding them. Any other effects the spell or ability has will still apply. If such an effect refers to the found cards, it can't see them in exile.")
        ruling("2020-11-10", "If a spell or ability instructs an opponent you control to search multiple zones including the library, you decide what cards they find in all zones. They'll exile all cards they find in any of the zones.")
        ruling("2020-11-10", "If more than one player controls an Opposition Agent, and another player searches their library, the controller of the Opposition Agent that most recently entered the battlefield controls that player during the search.")
        ruling("2020-11-10", "You'll still pay all costs for a spell cast this way, including additional costs. You may also pay alternative costs if any are available.")
    }
}
