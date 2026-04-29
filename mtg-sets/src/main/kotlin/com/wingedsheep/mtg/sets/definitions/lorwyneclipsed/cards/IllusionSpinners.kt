package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped
import com.wingedsheep.sdk.scripting.references.Player

val IllusionSpinners = card("Illusion Spinners") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Faerie Wizard"
    power = 4
    toughness = 3
    oracleText = "You may cast this spell as though it had flash if you control a Faerie.\n" +
        "Flying\n" +
        "This creature has hexproof as long as it's untapped. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    keywords(Keyword.FLYING)

    conditionalFlash = Exists(
        player = Player.You,
        zone = Zone.BATTLEFIELD,
        filter = GameObjectFilter.Permanent.withSubtype("Faerie")
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, StaticTarget.SourceCreature),
            condition = SourceIsUntapped
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Zoltan Boros"
        flavorText = "You can't catch what you can't perceive."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb4229a9-8df4-4adc-9d3e-acd2221fa3e9.jpg?1767957009"
    }
}
