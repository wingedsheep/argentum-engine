package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dawn to Dusk
 * {2}{W}{W}
 * Sorcery
 *
 * Choose one or both —
 * • Return target enchantment card from your graveyard to your hand.
 * • Destroy target enchantment.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val DawnToDusk = card("Dawn to Dusk") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Return target enchantment card from your graveyard to your hand.\n" +
        "• Destroy target enchantment."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Return target enchantment card from your graveyard to your hand") {
                val card = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
                effect = Effects.Move(target = card, destination = Zone.HAND)
            }
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Robbie Trevino"
        flavorText = "\"The numbers of Nyxborn among us have swelled. Is this a sign of the gods' favor, or does it portend something else?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/026ee3ff-bfd3-42d7-a4fa-2a6372599aef.jpg?1783939584"
    }
}
