package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Reaper King
 * {2/W}{2/U}{2/B}{2/R}{2/G}
 * Legendary Artifact Creature — Scarecrow
 * 6 / 6
 *
 * ({2/W} can be paid with any two mana or with {W}. This card's mana value is 10.)
 * Other Scarecrow creatures you control get +1/+1.
 * Whenever another Scarecrow you control enters, destroy target permanent.
 *
 * - The mana cost is monocoloured hybrid ("twobrid"); mana value 10 is derived from the written
 *   cost, so nothing is overridden here. The reminder text is part of the printed oracle text and
 *   stays in the card's oracle text.
 * - The lord clause says "Scarecrow **creatures**", so it filters creatures
 *   ([GameObjectFilter.Creature]); `excludeSelf = true` is the printed "Other".
 * - The trigger clause says only "another Scarecrow you control", with no "creature" — a bare
 *   tribal noun means *permanents*, so a Scarecrow permanent that isn't currently a creature
 *   still triggers it. That asymmetry between the two printed lines is why the lord uses
 *   `GameObjectFilter.Creature` and the trigger uses `GameObjectFilter.Permanent`.
 * - The trigger is mandatory and targets, so it must target a permanent if one exists — including
 *   the Reaper King itself or the Scarecrow that just entered when nothing else is on the board.
 */
val ReaperKing = card("Reaper King") {
    manaCost = "{2/W}{2/U}{2/B}{2/R}{2/G}"
    typeLine = "Legendary Artifact Creature — Scarecrow"
    power = 6
    toughness = 6
    oracleText = "({2/W} can be paid with any two mana or with {W}. This card's mana value is 10.)\n" +
        "Other Scarecrow creatures you control get +1/+1.\n" +
        "Whenever another Scarecrow you control enters, destroy target permanent."

    // Whenever another Scarecrow you control enters, destroy target permanent.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.SCARECROW).youControl(),
            binding = TriggerBinding.OTHER
        )
        val t = target("target", TargetPermanent())
        effect = Effects.Destroy(t)
    }

    // Other Scarecrow creatures you control get +1/+1.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.SCARECROW).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Jim Murray"
        flavorText = "It's harvest time."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/502740bf-0bff-4358-8996-1a27e5f0343f.jpg?1783942710"
    }
}
