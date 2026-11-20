package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Peak Eruption
 * {2}{R}
 * Sorcery
 * Destroy target Mountain. Peak Eruption deals 3 damage to that land's controller.
 */
val PeakEruption = card("Peak Eruption") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target Mountain. Peak Eruption deals 3 damage to that land's controller."

    spell {
        val mountain = target(TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)))
        // Molten Rain's ordering: deal the damage while the target is still on the battlefield
        // so its controller resolves, then destroy. Damage is dealt even if the Mountain survives
        // (indestructible), matching the ruling.
        effect = Effects.DealDamage(3, EffectTarget.TargetController) then
            Effects.Move(mountain, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Adam Paquette"
        flavorText = "\"Early olive buds? A good omen. Flock of crows? Not so good. Mountain exploding? " +
            "Bad. Very, very bad.\"\n—Hira, street oracle"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed0a00f7-aee0-4ab2-bab6-bc0949176a7a.jpg?1783939758"
        ruling(
            "2013-09-15",
            "Peak Eruption can target any land with the subtype Mountain, not just those named Mountain. " +
                "It can't target a land that doesn't have that subtype, even if the land can tap for {R}."
        )
        ruling(
            "2013-09-15",
            "If the target Mountain is an illegal target when Peak Eruption tries to resolve, it won't " +
                "resolve and none of its effects will happen. No damage will be dealt. However, if Peak " +
                "Eruption resolves and the Mountain isn't destroyed (perhaps because it has indestructible), " +
                "damage will be dealt."
        )
    }
}
