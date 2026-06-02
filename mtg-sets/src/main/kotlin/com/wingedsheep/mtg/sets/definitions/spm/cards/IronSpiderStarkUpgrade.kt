package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val IronSpiderStarkUpgrade = card("Iron Spider, Stark Upgrade") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Spider Hero"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "{T}: Put a +1/+1 counter on each artifact creature and/or Vehicle you control.\n" +
        "{2}, Remove two +1/+1 counters from among artifacts you control: Draw a card."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        val artifactCreaturesAndVehicles =
            (GameObjectFilter.Artifact and GameObjectFilter.Creature) or
                GameObjectFilter.Any.withSubtype("Vehicle")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(artifactCreaturesAndVehicles.youControl()),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Artifact, 2)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Kevin Glint"
        flavorText = "Peter appreciated the powerful new suit sent by Tony Stark, but he soon found it came with some strings attached."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8da5f34e-7f40-406a-88d2-bb1e3ed25200.jpg?1757378035"
    }
}
