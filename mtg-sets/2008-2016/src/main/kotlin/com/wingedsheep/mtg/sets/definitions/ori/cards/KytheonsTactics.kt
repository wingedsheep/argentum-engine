package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kytheon's Tactics
 * {1}{W}{W}
 * Sorcery
 *
 * Creatures you control get +2/+1 until end of turn.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * those creatures also gain vigilance until end of turn.
 *
 * Both group passes run within the same resolution and the pump can't change which creatures you
 * control, so the spell-mastery pass covers exactly "those creatures" the pump affected.
 */
val KytheonsTactics = card("Kytheon's Tactics") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+1 until end of turn.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "those creatures also gain vigilance until end of turn. (Attacking doesn't cause them to tap.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 1, EffectTarget.IterationEntity)
        ) then Effects.If(
            condition = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery),
            then = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0521669-206e-4909-be6f-a74603203a70.jpg?1783938359"
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
