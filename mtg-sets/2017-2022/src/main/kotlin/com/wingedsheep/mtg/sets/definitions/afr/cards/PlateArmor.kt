package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Plate Armor
 * {2}{W}
 * Artifact — Equipment
 * Equipped creature gets +3/+3 and has ward {1}.
 * Equip {3}. This ability costs {1} less to activate for each other Equipment you control.
 *
 * The equip discount rides `equipAbility`'s `genericCostReduction` (Dragonfire Blade's shape),
 * counting Equipment you control with `excludeSelf` so Plate Armor never discounts itself.
 */
val PlateArmor = card("Plate Armor") {
    manaCost = "{2}{W}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+3 and has ward {1}. (Whenever equipped creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Equip {3}. This ability costs {1} less to activate for each other Equipment you control. ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(3, 3, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"), Filters.EquippedCreature)
    }

    equipAbility(
        "{3}",
        genericCostReduction = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
            excludeSelf = true,
        ).count(),
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Martina Pilcerova"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad447e50-cfb1-40df-ae01-a65c46f2f572.jpg?1783926524"
        ruling("2021-07-23", "If a creature with ward is equipped with Plate Armor, becoming the target of a spell or ability an opponent controls will cause both ward abilities to trigger.")
        ruling("2021-07-23", "If a player casts a spell that targets multiple permanents their opponent controls with ward, each of those ward abilities will trigger. If that player doesn't pay for all of them, the spell will be countered.")
    }
}
