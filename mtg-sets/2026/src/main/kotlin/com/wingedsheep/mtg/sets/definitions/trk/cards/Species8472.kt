package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeTargetedBySourceTypeAbilities
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Species 8472
 * {4}{G}{G}
 * Creature — Insect Beast
 * 7/5
 *
 * Strange new worlds — When this creature enters, you gain life equal to the number of
 * differently named lands you control.
 * This creature can't be blocked by artifact creatures.
 * This creature can't be the target of abilities from artifact sources your opponents control.
 */
val Species8472 = card("Species 8472") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Insect Beast"
    power = 7
    toughness = 5
    oracleText = "Strange new worlds — When this creature enters, you gain life equal to the number of " +
        "differently named lands you control.\n" +
        "This creature can't be blocked by artifact creatures.\n" +
        "This creature can't be the target of abilities from artifact sources your opponents control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.differentlyNamedLandsYouControl())
    }

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.ArtifactCreature)
    }

    staticAbility {
        ability = CantBeTargetedBySourceTypeAbilities(
            sourceType = CardType.ARTIFACT,
            filter = GroupFilter.source(),
            sourceController = Player.EachOpponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d74723-4c9e-4fe7-8276-05d5f1b7ba0c.jpg?1793273864"
    }
}
