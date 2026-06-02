package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ardyn, the Usurper
 * {5}{B}{B}{B}
 * Legendary Creature — Elder Human Noble
 * 4/4
 *
 * Demons you control have menace, lifelink, and haste.
 * Starscourge — At the beginning of combat on your turn, exile up to one target creature
 * card from a graveyard. If you exiled a card this way, create a token that's a copy of
 * that card, except it's a 5/5 black Demon.
 *
 * "Starscourge" is an ability word with no rules meaning.
 */
val ArdynTheUsurper = card("Ardyn, the Usurper") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Elder Human Noble"
    power = 4
    toughness = 4
    oracleText = "Demons you control have menace, lifelink, and haste.\n" +
        "Starscourge — At the beginning of combat on your turn, exile up to one target creature " +
        "card from a graveyard. If you exiled a card this way, create a token that's a copy of " +
        "that card, except it's a 5/5 black Demon."

    // Demons you control have menace, lifelink, and haste.
    val demons = GroupFilter.AllCreaturesYouControl.withSubtype("Demon")
    staticAbility { ability = GrantKeyword(Keyword.MENACE, demons) }
    staticAbility { ability = GrantKeyword(Keyword.LIFELINK, demons) }
    staticAbility { ability = GrantKeyword(Keyword.HASTE, demons) }

    // Starscourge — At the beginning of combat on your turn, exile up to one target creature
    // card from a graveyard, then create a 5/5 black Demon token copy of it.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        // "Up to one target" — exile and copy both no-op if no target is chosen, which
        // naturally satisfies the oracle's "if you exiled a card this way" guard.
        val graveyardCard = target(
            "creature card from a graveyard",
            TargetObject(optional = true, filter = TargetFilter.CreatureInGraveyard)
        )
        effect = Effects.Composite(listOf(
            Effects.Exile(graveyardCard),
            Effects.CreateTokenCopyOfTarget(
                graveyardCard,
                overridePower = 5,
                overrideToughness = 5,
                overrideColors = setOf(Color.BLACK),
                overrideSubtypes = setOf(Subtype.DEMON)
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Russell Lu"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4627072e-9c72-4084-8021-690777342548.jpg?1748706094"

        ruling("2025-06-06", "Except for the listed exceptions, the token copies exactly what was printed on the original card and nothing else. It doesn't copy any information about the object the card was before it was put into a graveyard.")
        ruling("2025-06-06", "If a card copied by the token had any \"when [this permanent] enters\" abilities, the token also has those abilities, and they'll trigger when it's created. Similarly, any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities that the token has copied will also work.")
        ruling("2025-06-06", "If the copied card has {X} in its mana cost, X is 0.")
        ruling("2025-06-06", "The token is a Demon creature instead of its other types and is black instead of its other colors. Its base power and toughness are 5/5. These are copiable values of the token that other effects may copy.")
    }
}
