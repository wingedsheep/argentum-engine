package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Halimar Tidecaller
 * {2}{U}
 * Creature — Human Wizard Ally
 * 2/3
 * When this creature enters, you may return target card with awaken from your graveyard to your hand.
 * Land creatures you control have flying.
 *
 * - "Card with awaken" reads `Keyword.AWAKEN`, which `KeywordAbility.Awaken` surfaces on the card.
 * - The flying grant is a static over the projected "land creature you control" group (Tatyova's
 *   shape), so it reaches lands awakened or otherwise animated after the Tidecaller is in play.
 */
val HalimarTidecaller = card("Halimar Tidecaller") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard Ally"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you may return target card with awaken from your graveyard " +
        "to your hand.\nLand creatures you control have flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(
            TargetFilter(GameObjectFilter.Any.withKeyword(Keyword.AWAKEN).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(t, Zone.HAND)
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.FLYING,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Magali Villeneuve"
        flavorText = "\"Quiet the waves? Why would I want to do that?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc3e3b18-bf00-4f1a-9918-9a0e9d75b747.jpg?1783938208"
    }
}
