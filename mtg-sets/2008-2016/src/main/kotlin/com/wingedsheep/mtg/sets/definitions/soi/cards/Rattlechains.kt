package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rattlechains — Shadows over Innistrad #81
 * {1}{U} · Creature — Spirit · 2/1
 *
 * Flash
 * Flying
 * When this creature enters, target Spirit gains hexproof until end of turn.
 * You may cast Spirit spells as though they had flash.
 *
 * The flash grant is a [GrantFlashToSpellType] static, read only when casting spells
 * (`FlashTypeGrants`), so it never touches ability timing.
 */
val Rattlechains = card("Rattlechains") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\n" +
        "When this creature enters, target Spirit gains hexproof until end of turn.\n" +
        "You may cast Spirit spells as though they had flash."

    keywords(Keyword.FLASH, Keyword.FLYING)

    // When this creature enters, target Spirit gains hexproof until end of turn.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val spirit = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SPIRIT)))
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, spirit)
    }

    // You may cast Spirit spells as though they had flash.
    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any.withSubtype(Subtype.SPIRIT),
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6fc4db9-a29c-4f50-8e41-105b45af0be9.jpg?1783937791"
    }
}
