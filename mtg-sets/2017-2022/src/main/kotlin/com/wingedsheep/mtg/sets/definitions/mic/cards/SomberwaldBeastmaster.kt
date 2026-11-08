package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Somberwald Beastmaster — Innistrad: Midnight Hunt Commander #30 (canonical printing)
 * {6}{G} · Creature — Human Ranger · 1/1
 *
 * When this creature enters, create a 2/2 green Wolf creature token, a 3/3 green Beast creature
 * token, and a 4/4 green Beast creature token.
 * Creature tokens you control have deathtouch.
 *
 * The three tokens are created by one instruction, so they enter together as one token-creation
 * event (`CreateTokensTogether`), not three sequential ones.
 */
val SomberwaldBeastmaster = card("Somberwald Beastmaster") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, create a 2/2 green Wolf creature token, a 3/3 green Beast creature token, " +
        "and a 4/4 green Beast creature token.\n" +
        "Creature tokens you control have deathtouch. (Any amount of damage they deal to a creature is enough to destroy it.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTokensTogether(
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Wolf"),
                imageUri = "https://cards.scryfall.io/normal/front/3/6/364e04d9-9a8a-49df-921c-7a9bf62dc731.jpg?1783925220"
            ),
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Beast"),
                imageUri = "https://cards.scryfall.io/normal/front/a/6/a605be96-abae-4c11-8e7c-6fb87b4d8795.jpg?1783925228"
            ),
            Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Beast"),
                imageUri = "https://cards.scryfall.io/normal/front/f/5/f55c70b8-0fa5-4d08-9061-f53d6f949908.jpg?1783925222"
            ),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.DEATHTOUCH,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a4ba28b-a6c6-4268-8dc2-542d203f91a6.jpg?1783925373"
    }
}
