package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cliffhaven Kitesail — Zendikar Rising #243
 * {1} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature has flying.
 * Equip {2}
 */
val CliffhavenKitesail = card("Cliffhaven Kitesail") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature has flying.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Caroline Gariba"
        flavorText = "Made from canvas, wood, and hope."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eebf8f67-49d0-42de-a5cc-ad13d19d2c55.jpg"
    }
}
