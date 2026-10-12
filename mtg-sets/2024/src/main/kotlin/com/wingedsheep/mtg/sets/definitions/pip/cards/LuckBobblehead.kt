package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

val LuckBobblehead = card("Luck Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n{1}, {T}: Roll X six-sided dice, where X is the number of Bobbleheads you control. Create a tapped Treasure token for each even result. If you rolled 6 exactly seven times, you win the game."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        description = "Roll a six-sided die for each Bobblehead you control. Create a tapped Treasure for each even result. Exactly seven sixes wins the game."
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Pipeline {
            val (evens, sixes) = rollDice(
                count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
                sides = 6,
                resultSets = listOf(setOf(2, 4, 6), setOf(6)),
            )
            run(Effects.CreateTreasure(
                evens.amount,
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/1/d/1d74d012-66ca-4a0c-9e90-e31e96730d4d.jpg?1783912040",
            ))
            run(Effects.If(
                Conditions.CompareAmounts(sixes.amount, ComparisonOperator.EQ, 7),
                Effects.WinGame(),
            ))
        }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Ørjan Ruttenborg Svendsen"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13281945-47ff-464d-96ef-9b26ef6783fa.jpg?1783912379"
        ruling("2024-03-08", "The value of X is determined only once, as Luck Bobblehead’s last ability resolves.")
        ruling("2024-03-08", "You win the game only if you rolled 6 exactly seven times while resolving a single instance of Luck Bobblehead’s last ability. Results of previous die rolls, whether they’re from previous resolutions of Luck Bobblehead’s last ability or from other effects that instruct you to roll dice, don’t matter for the purposes of this effect.")
    }
}
