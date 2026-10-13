package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

val CharismaBobblehead = card("Charisma Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n{4}, {T}: Create X 1/1 white Soldier creature tokens, where X is the number of Bobbleheads you control. Activate only as a sorcery."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        description = "Create a 1/1 white Soldier token for each Bobblehead you control."
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/a/f/af191656-5e62-440f-b63a-705cfed314e7.jpg?1783912045",
        )
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Michał Miłkowski"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35f61330-869f-448c-8374-1cbac7c23454.jpg?1783912380"
        ruling("2024-03-08", "The value of X is calculated only once, as Charisma Bobblehead’s last ability resolves.")
    }
}
