package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Junkyard Genius
 * {1}{B}{R}
 * Creature — Human Artificer
 * 2/2
 * When this creature enters, create a tapped Powerstone token.
 * {1}{B}{R}, Sacrifice another creature or artifact: Until end of turn, other creatures you
 * control get +1/+0 and gain menace and haste.
 */
val JunkyardGenius = card("Junkyard Genius") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")\n" +
        "{1}{B}{R}, Sacrifice another creature or artifact: Until end of turn, other creatures you control get +1/+0 and gain menace and haste."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{R}"),
            Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        )
        // The affected set is locked in on resolution (CR 611.2c), so iterating the group once is faithful.
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity),
        )
        description = "{1}{B}{R}, Sacrifice another creature or artifact: Until end of turn, other creatures you control get +1/+0 and gain menace and haste."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Rémi Jacquot"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a15b0676-b698-4160-8d2f-a22a1011bac0.jpg?1783920029"
    }
}
