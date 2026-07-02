package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cloud, Planet's Champion
 * {3}{R}{W}
 * Legendary Creature — Human Soldier Mercenary
 * 4/4
 *
 * During your turn, as long as Cloud is equipped, it has double strike and indestructible.
 * Equip abilities you activate that target Cloud cost {2} less to activate.
 *
 * Ability 1 composes two conditional keyword grants (the same shape as Merry, Esquire of Rohan's
 * "has first strike as long as it's equipped", plus Freya Crescent's [Conditions.IsYourTurn] gate).
 * The condition is [Conditions.All] of "it's your turn" and "the source is equipped"
 * (`GameObjectFilter.Any.equipped()`), evaluated in static-ability projection.
 *
 * Ability 2 is the target-restricted form of the controller-scoped [ReduceEquipCost]
 * (Éowyn, Lady of Rohan): with [ReduceEquipCost.onlyIfTargetIsSource] the {2} reduction applies
 * only to equip abilities whose chosen target is Cloud itself.
 */
val CloudPlanetsChampion = card("Cloud, Planet's Champion") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier Mercenary"
    power = 4
    toughness = 4
    oracleText = "During your turn, as long as Cloud is equipped, it has double strike and indestructible. " +
        "(This creature deals both first-strike and regular combat damage. Damage and effects that say " +
        "\"destroy\" don't destroy this creature.)\n" +
        "Equip abilities you activate that target Cloud cost {2} less to activate."

    // "During your turn, as long as Cloud is equipped, it has double strike and indestructible."
    val whileEquippedOnYourTurn = Conditions.All(
        Conditions.IsYourTurn,
        Conditions.SourceMatches(GameObjectFilter.Any.equipped())
    )
    staticAbility {
        condition = whileEquippedOnYourTurn
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.source())
    }
    staticAbility {
        condition = whileEquippedOnYourTurn
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source())
    }

    // "Equip abilities you activate that target Cloud cost {2} less to activate."
    staticAbility {
        ability = ReduceEquipCost(amount = 2, onlyIfTargetIsSource = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "552"
        artist = "Magali Villeneuve"
        flavorText = "\"It's not over yet... this isn't the end!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6d58067-337d-43dc-b4a3-c6acc701d450.jpg?1782686131"
    }
}
