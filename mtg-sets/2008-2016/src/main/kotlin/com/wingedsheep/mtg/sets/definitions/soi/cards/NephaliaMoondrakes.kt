package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nephalia Moondrakes (Shadows over Innistrad #75)
 * {5}{U}{U}
 * Creature — Drake
 * 5/5
 *
 * Flying
 * When this creature enters, target creature gains flying until end of turn.
 * {4}{U}{U}, Exile this card from your graveyard: Creatures you control gain flying until end of turn.
 */
val NephaliaMoondrakes = card("Nephalia Moondrakes") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, target creature gains flying until end of turn.\n" +
        "{4}{U}{U}, Exile this card from your graveyard: Creatures you control gain flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U}{U}"), Costs.ExileSelf)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity)
        )
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Tianhua X"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f714d6c4-b1e1-40dd-806f-a3d87020292e.jpg"
    }
}
