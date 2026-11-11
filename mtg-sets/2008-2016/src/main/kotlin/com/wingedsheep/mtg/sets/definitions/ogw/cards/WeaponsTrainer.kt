package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Weapons Trainer — Oath of the Gatewatch #160 (canonical printing)
 * {R}{W} · Creature — Human Soldier Ally · 3/2
 *
 * Other creatures you control get +1/+0 as long as you control an Equipment.
 *
 * Nadaar's conditional lord shape: [ModifyStats] over [GroupFilter.OtherCreaturesYouControl]
 * gated by [Conditions.YouControl] an Equipment. The condition is a plain existence check, so a
 * second Equipment adds nothing and an unattached Equipment still counts (both 2020-08-07 rulings).
 */
val WeaponsTrainer = card("Weapons Trainer") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier Ally"
    power = 3
    toughness = 2
    oracleText = "Other creatures you control get +1/+0 as long as you control an Equipment."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 0,
                filter = GroupFilter.OtherCreaturesYouControl
            ),
            condition = Conditions.YouControl(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Greg Opalinski"
        flavorText = "\"When I'm done training you, anything will be a deadly weapon.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d47bcb1-b028-4be8-9710-7c390cb984ce.jpg?1783937895"
        ruling("2020-08-07", "Weapons Trainer's ability gives just +1/+0, no matter how many Equipment you control beyond the first.")
        ruling("2020-08-07", "Weapons Trainer's ability applies even if the Equipment you control isn't attached to any creature.")
    }
}
