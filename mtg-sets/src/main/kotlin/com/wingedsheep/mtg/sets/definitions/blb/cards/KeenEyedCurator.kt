package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

// Keen-Eyed Curator - {G}{G}
// Creature — Raccoon Scout - 3/3
// As long as there are four or more card types among cards exiled with this creature,
// it gets +4/+4 and has trample.
// {1}: Exile target card from a graveyard.
val KeenEyedCurator = card("Keen-Eyed Curator") {
    manaCost = "{G}{G}"
    typeLine = "Creature — Raccoon Scout"
    power = 3
    toughness = 3
    oracleText = "As long as there are four or more card types among cards exiled with this creature, it gets +4/+4 and has trample.\n{1}: Exile target card from a graveyard."

    val fourOrMoreCardTypes = Compare(
        DynamicAmount.CardTypesInLinkedExile,
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(4)
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(4, 4, GroupFilter.source()),
            condition = fourOrMoreCardTypes
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = fourOrMoreCardTypes
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target("card in a graveyard", Targets.CardInGraveyard)
        effect = MoveToZoneEffect(
            target = t,
            destination = Zone.EXILE,
            linkToSource = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "PINDURSKI"
        flavorText = "\"Now this one's a real collector's item. They say Lily Emberseed herself drank from this mug when she was still Lily of Valley.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cf33d80-0704-4dc4-8e8d-1dcbcbc35add.jpg?1721426856"
        ruling("2024-07-26", "The card types that can appear on cards exiled with Keen-Eyed Curator are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are supertypes, not card types. Raccoon and Scout are subtypes, not card types.")
    }
}
