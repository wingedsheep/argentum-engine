package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trial of Solidarity
 * {2}{W}
 * Enchantment
 * When this enchantment enters, creatures you control get +2/+1 and gain vigilance until end of turn.
 * When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
val TrialOfSolidarity = card("Trial of Solidarity") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, creatures you control get +2/+1 and gain vigilance until end of turn.\n" +
        "When a Cartouche you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.ModifyStats(2, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cartouche").youControl()).enters()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Daniel Ljunggren"
        flavorText = "\"You will succeed as one or fail as many.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2604d43-a105-440b-86b1-60d1d0c04339.jpg"
    }
}
