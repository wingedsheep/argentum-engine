package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bat'leth
 * {2}{B}
 * Artifact — Equipment
 *
 * When this Equipment enters, target creature an opponent controls gets -2/-2 until end of turn.
 * Equipped creature gets +2/+0.
 * Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)
 */
val Batleth = card("Bat'leth") {
    manaCost = "{2}{B}"
    typeLine = "Artifact — Equipment"
    colorIdentity = "B"
    oracleText = "When this Equipment enters, target creature an opponent controls gets -2/-2 until end of turn.\nEquipped creature gets +2/+0.\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(-2, -2, creature)
    }
    equipAbility("{2}")
    staticAbility { ability = ModifyStats(2, 0) }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Tony Foti"
        flavorText = "\"Your tongue is as sharp as a warrior's! Let's see if your blade is as well!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/888e6e0f-43c3-4516-88ff-8a8d814478d4.jpg?1785981448"
    }
}
