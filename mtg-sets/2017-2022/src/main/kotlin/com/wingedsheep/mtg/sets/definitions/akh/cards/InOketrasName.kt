package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * In Oketra's Name
 * {1}{W}
 * Instant
 * Zombies you control get +2/+1 until end of turn. Other creatures you control get +1/+1 until
 * end of turn.
 *
 * "Other creatures" means creatures *other than those Zombies* — i.e. your non-Zombie creatures —
 * so each creature you control gets exactly one of the two bonuses. The bare noun "Zombies" is
 * Zombie permanents you control.
 */
val InOketrasName = card("In Oketra's Name") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Zombies you control get +2/+1 until end of turn. Other creatures you control get +1/+1 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()),
            Effects.ModifyStats(2, 1, EffectTarget.IterationEntity),
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.ZOMBIE).youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Nils Hamm"
        flavorText = "\"A single grain of sand is insignificant, but a sandstorm can reduce pyramids to dust. Stand with your crop. Become the storm.\"\n—Oketra, god of solidarity"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccf535ef-79ee-4a31-a614-f6c77fea72b5.jpg"
    }
}
