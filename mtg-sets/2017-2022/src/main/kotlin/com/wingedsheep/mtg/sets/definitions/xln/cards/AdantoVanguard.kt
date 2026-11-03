package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adanto Vanguard
 * {1}{W}
 * Creature — Vampire Soldier
 * 1/1
 * As long as this creature is attacking, it gets +2/+0.
 * Pay 4 life: This creature gains indestructible until end of turn.
 *
 * The attacking bonus is Grasping Scoundrel's conditional [ModifyStats] on
 * [Conditions.SourceIsAttacking]; the life-payment ability is Marrow Bats' bare
 * [Costs.PayLife] cost with an end-of-turn indestructible grant on itself.
 */
val AdantoVanguard = card("Adanto Vanguard") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 1
    toughness = 1
    oracleText = "As long as this creature is attacking, it gets +2/+0.\n" +
        "Pay 4 life: This creature gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    staticAbility {
        ability = ModifyStats(2, 0, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    activatedAbility {
        cost = Costs.PayLife(4)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21c950d7-b4f6-4902-8c9a-98f2933f9fa5.jpg?1783935808"
        ruling(
            "2019-01-25",
            "An ability that triggers whenever a creature with a certain power attacks uses " +
                "Adanto Vanguard's power after it has +2/+0 to determine whether it triggers."
        )
    }
}
