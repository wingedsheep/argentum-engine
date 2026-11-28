package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silver-Inlaid Dagger
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +2/+0.
 * As long as equipped creature is a Human, it gets an additional +1/+0.
 * Equip {2}
 *
 * Heavy Mattock's shape: the additional bonus is a second [ModifyStats] static gated by
 * [Conditions.EntityMatches] on the equipped creature being a Human.
 */
val SilverInlaidDagger = card("Silver-Inlaid Dagger") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "As long as equipped creature is a Human, it gets an additional +1/+0.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
        )
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Austin Hsu"
        flavorText = "Magical etchings turned a whittler's tool into a wolfhunter's weapon."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8b8162a-68f0-45df-bb25-8fd4487257a4.jpg?1783940899"
    }
}
