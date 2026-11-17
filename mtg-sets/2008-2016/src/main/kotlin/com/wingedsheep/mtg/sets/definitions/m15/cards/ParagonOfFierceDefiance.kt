package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paragon of Fierce Defiance
 * {3}{R}
 * Creature — Human Warrior
 * 2/2
 * Other red creatures you control get +1/+1.
 * {R}, {T}: Another target red creature you control gains haste until end of turn. (It can attack and {T} this turn.)
 */
val ParagonOfFierceDefiance = card("Paragon of Fierce Defiance") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Other red creatures you control get +1/+1.\n" +
        "{R}, {T}: Another target red creature you control gains haste until end of turn. (It can attack and {T} this turn.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.RED).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.withColor(Color.RED).youControl().other())
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ad97067-2b6b-4e60-817c-6414e2739806.jpg"
    }
}
