package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Kamahl, Fist of Krosa
 * {4}{G}{G}
 * Legendary Creature — Human Druid
 * 4/3
 * {G}: Target land becomes a 1/1 creature until end of turn. It's still a land.
 * {2}{G}{G}{G}: Creatures you control get +3/+3 and gain trample until end of turn.
 */
val KamahlFistOfKrosa = card("Kamahl, Fist of Krosa") {
    manaCost = "{4}{G}{G}"
    typeLine = "Legendary Creature — Human Druid"
    power = 4
    toughness = 3
    oracleText = "{G}: Target land becomes a 1/1 creature until end of turn. It's still a land.\n{2}{G}{G}{G}: Creatures you control get +3/+3 and gain trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{G}")
        target = Targets.Land
        effect = Effects.AnimateLand(EffectTarget.ContextTarget(0))
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}{G}")
        effect = Effects.Composite(
            ForEachInGroupEffect(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                effect = ModifyStatsEffect(3, 3, EffectTarget.Self)
            ),
            ForEachInGroupEffect(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                effect = GrantKeywordUntilEndOfTurnEffect(Keyword.TRAMPLE, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Matthew D. Wilson"
        flavorText = "\"My mind has changed. My strength has not.\""
        imageUri = "https://cards.scryfall.io/large/front/1/5/150d5229-b1a5-42cf-bf6a-04d246f1124f.jpg?1562900066"
    }
}
