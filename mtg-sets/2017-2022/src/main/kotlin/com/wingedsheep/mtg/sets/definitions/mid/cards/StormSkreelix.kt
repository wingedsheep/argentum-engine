package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm Skreelix
 * {3}{U}{R}
 * Creature — Drake Horror
 * 2/4
 * Flying
 * Instant and sorcery spells you cast cost {1} less to cast.
 * Whenever you cast an instant or sorcery spell, this creature gets +2/+0 until end of turn.
 */
val StormSkreelix = card("Storm Skreelix") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Drake Horror"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "Whenever you cast an instant or sorcery spell, this creature gets +2/+0 until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c49618f-2e9e-4674-9953-153ccc76cce2.jpg?1783925553"
    }
}
