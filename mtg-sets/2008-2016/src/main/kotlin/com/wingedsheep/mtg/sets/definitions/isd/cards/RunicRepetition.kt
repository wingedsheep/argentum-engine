package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Runic Repetition
 * {2}{U}
 * Sorcery
 * Return target exiled card with flashback you own to your hand.
 *
 * Same target shape as Rootcoil Creeper's activated ability: a card in exile with the FLASHBACK
 * keyword, owned by you. Flashback granted by another effect (Snapcaster Mage style) doesn't
 * survive the card leaving the stack, so only printed flashback counts — matching the ruling.
 */
val RunicRepetition = card("Runic Repetition") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target exiled card with flashback you own to your hand."

    spell {
        val exiled = target(
            TargetFilter(GameObjectFilter.Any.withKeyword(Keyword.FLASHBACK).ownedByYou(), zone = Zone.EXILE)
        )
        effect = Effects.ReturnToHand(exiled)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Svetlin Velinov"
        flavorText = "\"What some call obsession, I call the quest for perfection.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53e47ba6-3a55-41b4-b8fe-580041669408.jpg?1783940967"
        ruling("2011-09-22", "The card could have been exiled for any reason, not just because it was cast using flashback.")
        ruling("2011-09-22", "An effect that gives flashback to an instant or sorcery card in your graveyard stops applying once that card has left the stack. The card won't have flashback while exiled and can't be the target of Runic Repetition (unless it naturally has flashback).")
        ruling("2011-09-22", "A card that's exiled face down doesn't have any characteristics or abilities, so it can't be the target of Runic Repetition.")
    }
}
