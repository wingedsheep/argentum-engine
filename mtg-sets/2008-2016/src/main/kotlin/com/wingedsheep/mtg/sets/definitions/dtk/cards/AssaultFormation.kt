package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Assault Formation
 * {1}{G}
 * Enchantment
 *
 * Each creature you control assigns combat damage equal to its toughness rather than its power.
 * {G}: Target creature with defender can attack this turn as though it didn't have defender.
 * {2}{G}: Creatures you control get +0/+1 until end of turn.
 *
 * The static is Belligerent Brontodon's unconditional [AssignDamageEqualToToughness] over
 * [GroupFilter.AllCreaturesYouControl]. The defender lift is Hightide Hermit's
 * [Effects.CanAttackDespiteDefenderThisTurn] pointed at a target instead of the source (Clear a
 * Path's "creature with defender" filter), and the toughness pump is Banners Raised's group shape.
 */
val AssaultFormation = card("Assault Formation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Each creature you control assigns combat damage equal to its toughness rather than its power.\n" +
        "{G}: Target creature with defender can attack this turn as though it didn't have defender.\n" +
        "{2}{G}: Creatures you control get +0/+1 until end of turn."

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.AllCreaturesYouControl,
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        val creature = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)))
        effect = Effects.CanAttackDespiteDefenderThisTurn(creature)
        description = "{G}: Target creature with defender can attack this turn as though it didn't have defender."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Patterns.Group.modifyStatsForAll(
            0, 1,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
        description = "{2}{G}: Creatures you control get +0/+1 until end of turn."
    }

    metadata {
        ruling(
            "2017-11-17",
            "Assault Formation's first ability doesn't actually change any creature's power. It changes only " +
                "the amount of combat damage it assigns. All other rules and effects that check power or " +
                "toughness use the real values. For example, Hunt the Weak won't cause a creature to fight " +
                "with its toughness."
        )
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f672dd0-cd63-464c-9581-0ec6f0e391f7.jpg?1783938582"
    }
}
