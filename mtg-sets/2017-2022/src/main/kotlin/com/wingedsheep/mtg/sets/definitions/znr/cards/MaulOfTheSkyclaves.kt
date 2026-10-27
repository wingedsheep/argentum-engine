package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Maul of the Skyclaves — Zendikar Rising #27 (canonical printing; reprinted in March of the
 * Machine Commander)
 * {2}{W} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+2 and has flying and first strike.
 * Equip {2}{W}{W}
 */
val MaulOfTheSkyclaves = card("Maul of the Skyclaves") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+2 and has flying and first strike.(It deals combat damage before " +
        "creatures without first strike.)\n" +
        "Equip {2}{W}{W}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    equipAbility("{2}{W}{W}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f18f2608-0d44-442c-97a1-12ac94b7abac.jpg"
    }
}
