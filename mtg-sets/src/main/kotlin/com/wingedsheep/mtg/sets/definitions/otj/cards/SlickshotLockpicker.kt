package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Slickshot Lockpicker
 * {2}{U}
 * Creature — Human Rogue
 * 2/3
 * When this creature enters, target instant or sorcery card in your graveyard gains flashback
 * until end of turn. The flashback cost is equal to its mana cost.
 * Plot {2}{U}
 *
 * The ETB grants Flashback (CR 702.34) at runtime via [Effects.GrantFlashback], whose cost defaults
 * to the targeted card's own mana cost (matching "the flashback cost is equal to its mana cost").
 * Plot is the standard [KeywordAbility.plot] exile-and-cast-later mechanic.
 */
val SlickshotLockpicker = card("Slickshot Lockpicker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, target instant or sorcery card in your graveyard gains " +
        "flashback until end of turn. The flashback cost is equal to its mana cost. (You may cast " +
        "that card from your graveyard for its flashback cost. Then exile it.)\n" +
        "Plot {2}{U} (You may pay {2}{U} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()
        )
        effect = Effects.GrantFlashback()
        description = "When this creature enters, target instant or sorcery card in your graveyard " +
            "gains flashback until end of turn. The flashback cost is equal to its mana cost."
    }

    keywordAbility(KeywordAbility.plot("{2}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Wei Wei"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/109a9464-ce30-4747-874e-3bbf75913081.jpg?1712355500"
    }
}
