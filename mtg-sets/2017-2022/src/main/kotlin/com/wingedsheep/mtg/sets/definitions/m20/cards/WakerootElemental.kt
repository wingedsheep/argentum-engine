package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wakeroot Elemental
 * {4}{G}{G}
 * Creature — Elemental
 * 5/5
 *
 * {G}{G}{G}{G}{G}: Untap target land you control. It becomes a 5/5 Elemental creature with haste.
 * It's still a land. (This effect lasts as long as that land remains on the battlefield.)
 *
 * Nissa, Who Shakes the World's +1 shape: [Effects.Untap] then a [Duration.Permanent]
 * [Effects.BecomeCreature] — the reminder text says the animation never wears off. BecomeCreature
 * adds CREATURE and the Elemental subtype without removing LAND, so "it's still a land" is its
 * default. The 5/5 is a Layer 7b base P/T set, so counters and pumps keep applying (2019-07-12 ruling).
 */
val WakerootElemental = card("Wakeroot Elemental") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "{G}{G}{G}{G}{G}: Untap target land you control. It becomes a 5/5 Elemental " +
        "creature with haste. It's still a land. (This effect lasts as long as that land remains " +
        "on the battlefield.)"

    activatedAbility {
        cost = Costs.Mana("{G}{G}{G}{G}{G}")
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.Untap(land) then
            Effects.BecomeCreature(
                target = land,
                power = 5,
                toughness = 5,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf(Subtype.ELEMENTAL.value),
                duration = Duration.Permanent,
            )
        description = "{G}{G}{G}{G}{G}: Untap target land you control. It becomes a 5/5 Elemental " +
            "creature with haste. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Filip Burburan"
        flavorText = "\"When the forest walks, it's too late to run.\"\n—Horent, Khalni druid"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02c9396d-28f2-40b7-908e-51db51da57a7.jpg?1783932955"
        ruling(
            "2019-07-12",
            "Wakeroot Elemental's ability can target a land that's already a creature or a land " +
                "that's already untapped. The ability will untap it if possible and set its base " +
                "power and toughness to 5/5. Any counters or effects modifying that creature's " +
                "power and toughness will continue to apply, and any effects that set the " +
                "creature's power and toughness will be overwritten."
        )
    }
}
