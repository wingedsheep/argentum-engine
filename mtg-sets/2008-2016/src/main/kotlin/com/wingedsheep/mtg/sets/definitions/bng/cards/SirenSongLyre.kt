package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Siren Song Lyre
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature has "{2}, {T}: Tap target creature."
 * Equip {2}
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val SirenSongLyre = card("Siren Song Lyre") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{2}, {T}: Tap target creature.\"\nEquip {2}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            }
            // filter defaults to GroupFilter.attachedCreature() — "equipped creature has ..."
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "James Paick"
        flavorText = "A siren once tried to steal it out of jealousy but was caught by the song of its strings."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfeca720-3163-4391-b90d-5f7aa5f5987b.jpg?1783939520"
    }
}
