package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.values.ManaPayment

val MrHousePresidentAndCEO = card("Mr. House, President and CEO") {
    manaCost = "{R}{W}{B}"
    typeLine = "Legendary Artifact Creature — Human"
    power = 0
    toughness = 4
    oracleText = "Whenever you roll a 4 or higher, create a 3/3 colorless Robot artifact creature token. If you rolled 6 or higher, instead create that token and a Treasure token.\n{4}, {T}: Roll a six-sided die plus an additional six-sided die for each mana from Treasures spent to activate this ability."
    triggeredAbility {
        trigger = Triggers.you.rollsDie(minimumResult = 4)
        val robot = Effects.CreateToken(
            power = 3, toughness = 3, creatureTypes = setOf("Robot"),
            artifactToken = true, imageUri = "https://cards.scryfall.io/normal/front/7/6/76cde21f-2f01-4560-972b-7ecd6740d6de.jpg?1783912042",
        )
        effect = Effects.If(
            Conditions.CompareAmounts(DynamicAmounts.triggerDieResult(), ComparisonOperator.GTE, 6),
            Effects.CreateTokensTogether(
                robot,
                Effects.CreateTreasure(imageUri = "https://cards.scryfall.io/normal/front/1/d/1d74d012-66ca-4a0c-9e90-e31e96730d4d.jpg?1783912040"),
            ),
            otherwise = robot,
        )
    }
    activatedAbility {
        description = "Roll a six-sided die, plus one for each Treasure mana spent"
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.RollDice(count = DynamicAmounts.fixed(1) +
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION))
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23eb3cf7-c90d-4bfa-b125-4fbcb5614468.jpg?1783912421"
        ruling("2024-03-08", "Some cards from other releases may instruct you to roll dice other than six-sided dice. Results of 4 or higher on those dice cause Mr. House's first ability to trigger.")
        ruling("2024-03-08", "The die rolls caused by Mr. House's last ability have no inherent effect. Abilities that trigger whenever you roll dice or whenever you roll a certain result, such as Mr. House's first ability, will trigger as appropriate.")
    }
}
