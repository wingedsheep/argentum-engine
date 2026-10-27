package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Utility Knife — Zendikar Rising #256
 * {1} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+1.
 * Equip {3}
 */
val UtilityKnife = card("Utility Knife") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+1.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "256"
        artist = "Craig J Spearing"
        flavorText = "\"Hold on a second, I have just the thing.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5c781d-cd89-4671-8fbe-fae82c33357b.jpg"
    }
}
