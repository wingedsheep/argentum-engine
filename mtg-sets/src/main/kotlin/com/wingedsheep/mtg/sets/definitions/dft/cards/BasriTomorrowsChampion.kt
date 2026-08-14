package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** Basri, Tomorrow's Champion — Aetherdrift #3. */
val BasriTomorrowsChampion = card("Basri, Tomorrow's Champion") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 1
    oracleText = "{W}, {T}, Exert Basri: Create a 1/1 white Cat creature token with lifelink. " +
        "(An exerted creature won't untap during your next untap step.)\n" +
        "Cycling {2}{W} ({2}{W}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, Cats you control gain hexproof and indestructible until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap, Costs.Exert)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/902fba98-92b7-461b-ac6e-29cb2c4e307f.jpg?1783907681",
        )
        description = "{W}, {T}, Exert Basri: Create a 1/1 white Cat creature token with lifelink."
    }

    keywordAbility(KeywordAbility.cycling("{2}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val cats = GroupFilter(GameObjectFilter.Creature.withSubtype("Cat").youControl())
        effect = Effects.Composite(
            Patterns.Group.grantKeywordToAll(Keyword.HEXPROOF, cats),
            Patterns.Group.grantKeywordToAll(Keyword.INDESTRUCTIBLE, cats),
        )
        description = "When you cycle this card, Cats you control gain hexproof and indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/991270fa-a391-4c2e-bd9a-19151386fb67.jpg?1783907922"
    }
}
