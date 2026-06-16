package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pull from the Grave
 * {2}{B}
 * Sorcery
 * Return up to two target creature cards from your graveyard to your hand. You gain 2 life.
 */
val PullFromTheGrave = card("Pull from the Grave") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to your hand. You gain 2 life."
    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD),
        )
        effect = Effects.Composite(
            ForEachTargetEffect(
                effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)),
            ),
            GainLifeEffect(2),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Pauline Voss"
        flavorText = "\"A little bruised, a little broken, but at least no longer dead.\"\n—Dina"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d73612fe-8992-4650-a7e3-c7b662da6a03.jpg?1775937572"
    }
}
