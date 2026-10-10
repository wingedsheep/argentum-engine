package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Titania, Protector of Argoth — Commander 2014 #50
 * {3}{G}{G} · Legendary Creature — Elemental · 5/3
 *
 * When Titania enters, return target land card from your graveyard to the battlefield.
 * Whenever a land you control is put into a graveyard from the battlefield, create a 5/3 green
 * Elemental creature token.
 *
 * The return is the guarded [Effects.PutOntoBattlefieldFromGraveyard] (Molderhulk's shape). The
 * second ability is any land you control moving battlefield → graveyard (`Triggers.a(...).dies()`,
 * Dingus Egg's event narrowed by `youControl()`, read off last-known control).
 */
val TitaniaProtectorOfArgoth = card("Titania, Protector of Argoth") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental"
    power = 5
    toughness = 3
    oracleText = "When Titania enters, return target land card from your graveyard to the battlefield.\n" +
        "Whenever a land you control is put into a graveyard from the battlefield, create a 5/3 green " +
        "Elemental creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(land)
        description = "When Titania enters, return target land card from your graveyard to the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).dies()
        effect = Effects.CreateToken(
            power = 5,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/1/4/1449862b-309e-4c58-ac94-13d1acdd363f.jpg?1783938795",
        )
        description = "Whenever a land you control is put into a graveyard from the battlefield, create a 5/3 " +
            "green Elemental creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "50"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/224d904a-5972-4152-878a-9a922e7a55b6.jpg?1783938864"
    }
}
