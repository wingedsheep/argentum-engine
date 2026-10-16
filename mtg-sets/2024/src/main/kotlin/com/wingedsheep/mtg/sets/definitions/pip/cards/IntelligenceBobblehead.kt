package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Intelligence Bobblehead — X (Bobbleheads you control) is counted once, as the draw ability
 * resolves, per the 2024-03-08 ruling.
 */
val IntelligenceBobblehead = card("Intelligence Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{5}, {T}: Draw X cards, where X is the number of Bobbleheads you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        description = "Draw a card for each Bobblehead you control."
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Torgeir Fjereide"
        flavorText = "Bobbleheads were once an exclusive perk for Vault-Tec executives and residential vault dwellers."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6546c0d-6be2-4ced-9d7c-4269df8dea76.jpg?1783912379"
        ruling("2024-03-08", "The value of X is determined only once, as Intelligence Bobblehead’s last ability resolves.")
    }
}
