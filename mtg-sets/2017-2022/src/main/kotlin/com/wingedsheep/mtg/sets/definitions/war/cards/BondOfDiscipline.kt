package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bond of Discipline
 * {4}{W}
 * Sorcery
 * Tap all creatures your opponents control. Creatures you control gain lifelink until end of turn.
 */
val BondOfDiscipline = card("Bond of Discipline") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Tap all creatures your opponents control. Creatures you control gain lifelink until end of turn."

    spell {
        effect = Patterns.Group.tapAll(GroupFilter.AllCreaturesOpponentsControl) then
            Patterns.Group.grantKeywordToAll(Keyword.LIFELINK, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Zezhou Chen"
        flavorText = "\"We agree that order benefits everyone, but not until you enforce it.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb7c78bb-9f2a-47a4-adc4-b497bb38f46f.jpg?1783933486"
    }
}
