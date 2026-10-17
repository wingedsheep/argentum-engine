package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Take to the Streets — Streets of New Capenna #158
 * {4}{G} · Sorcery
 *
 * Creatures you control get +2/+2 until end of turn. Citizens you control get an additional
 * +1/+1 and gain vigilance until end of turn.
 *
 * The Glistening Deluge shape: two [Effects.ForEachInGroup] passes, each locked in once at
 * resolution — the whole team first, then the Citizens among them for the extra +1/+1 and
 * vigilance. Both grants are end-of-turn and land on the iteration entity.
 */
val TakeToTheStreets = card("Take to the Streets") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+2 until end of turn. Citizens you control get an additional +1/+1 and gain vigilance until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity),
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Citizen").youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Matt Forsyth"
        flavorText = "No institution, however grand, can withstand the collective will of the people."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b4a5122-6922-4ab5-aa43-94cf2d7f9c8a.jpg?1783923101"
    }
}
