package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Decimator of the Provinces
 * {10}
 * Creature — Eldrazi Boar
 * 7/7
 *
 * Emerge {6}{G}{G}{G}
 * When you cast this spell, creatures you control get +2/+2 and gain trample until end of turn.
 * Trample, haste
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper.
 * - The pump is a *cast* trigger: it resolves while this is still on the stack, so the Boar itself
 *   is NOT among "creatures you control" and doesn't get +2/+2 — it just brings its own trample and
 *   haste. That's the whole point of the card (swing with the team the turn it lands).
 */
val DecimatorOfTheProvinces = card("Decimator of the Provinces") {
    manaCost = "{10}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Boar"
    power = 7
    toughness = 7
    oracleText = "Emerge {6}{G}{G}{G} (You may cast this spell by sacrificing a creature and " +
        "paying the emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, creatures you control get +2/+2 and gain trample until end of " +
        "turn.\nTrample, haste"

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    emerge("{6}{G}{G}{G}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.ModifyStats(2, 2, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ),
        )
        description = "When you cast this spell, creatures you control get +2/+2 and gain " +
            "trample until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/587cb384-db24-4e3d-a338-230e50305d31.jpg?1783937529"
    }
}
