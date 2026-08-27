package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soulblade Djinn
 * {3}{U}{U}
 * Creature — Djinn
 * 4/3
 * Flying
 * Whenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn.
 */
val SoulbladeDjinn = card("Soulblade Djinn") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 4
    toughness = 3
    oracleText = "Flying\nWhenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Viktor Titov"
        flavorText = "He grants endless wishes, as long as you always wish for a blade."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a48455-dde4-4263-9146-af547c0aad48.jpg?1783938347"

        ruling("2015-06-22", "Any spell you cast that doesn't have the type creature will cause Soulblade Djinn's ability to trigger. If a spell has multiple types, and one of those types is creature (such as an artifact creature), casting it won't cause the ability to trigger. Playing a land also won't cause it to trigger.")
        ruling("2015-06-22", "Soulblade Djinn's ability goes on the stack on top of the spell that caused it to trigger. It will resolve before that spell.")
    }
}
