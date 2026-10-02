package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.OptionalEffectDiscardDestination
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

val LibraryOfLeng = card("Library of Leng") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "You have no maximum hand size.\nIf an effect causes you to discard a card, discard it, but you may put it on top of your library instead of into your graveyard."

    staticAbility { ability = NoMaximumHandSize }
    replacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2340edcb-8cd5-4ccd-99e2-b9a29f72c495.jpg?1783948664"
        ruling("2009-10-01", "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put Null Profusion (an enchantment that says your maximum hand size is two) onto the battlefield and then put Library of Leng onto the battlefield, you'll have no maximum hand size. However, if those permanents entered in the opposite order, your maximum hand size would be two.")
        ruling("2004-10-04", "You can't use the Library of Leng ability to place a discarded card on top of your library when you discard a card as a cost, because costs aren't effects.")
        ruling("2004-10-04", "The discard triggers anything else that triggers on discards.")
        ruling("2004-10-04", "You can look at a randomly discarded card before deciding where it goes.")
        ruling("2004-10-04", "This effect has no effect on the cards being put into the graveyard from a library, because they are not \"discarded\".")
        ruling("2004-10-04", "If more than one card is discarded due to a single effect, the Library allows you to decide whether or not to use it on each of the cards. You get to decide the order the cards are placed on the library if more than one goes there.")
        ruling("2004-10-04", "Since the card goes directly to the library, the card is not revealed unless the spell or ability requiring the discard specifically says it is.")
        ruling("2004-10-04", "The ability replaces the normal discard action with a discard action that puts the card on the library instead of the graveyard.")
        ruling("2004-10-04", "The ability applies any time a spell or ability has you discard as part of its effect. It does not matter if you or your opponent control the spell or ability. The discard is forced because it is an effect.")
    }
}
