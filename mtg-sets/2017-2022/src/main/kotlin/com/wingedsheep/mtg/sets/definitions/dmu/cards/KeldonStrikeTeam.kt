package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val KeldonStrikeTeam = card("Keldon Strike Team") {
    manaCost = "{2}{R}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 1
    oracleText = "Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, create two 1/1 white Soldier creature tokens.\n" +
        "As long as this creature entered this turn, creatures you control have haste."

    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/8/c/8c4b0257-2ca5-4015-9d63-d7cf6e87ab9d.jpg?1783921132",
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl),
            condition = Conditions.SourceEnteredThisTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4af4031e-b14e-4568-b047-b3d470d9b18e.jpg?1783921314"
    }
}
