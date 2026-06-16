package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bruse Tarl, Roving Rancher
 * {2}{R}{W}
 * Legendary Creature — Human Warrior
 * 4/3
 *
 * Oxen you control have double strike.
 * Whenever Bruse Tarl enters or attacks, exile the top card of your library. If it's a
 * land card, create a 2/2 white Ox creature token. Otherwise, you may cast it until the
 * end of your next turn.
 *
 * Implementation:
 * - Lord: [GrantKeyword] of double strike to Oxen you control (mirrors Mobile Homestead's
 *   keyword-grant lord shape).
 * - "Enters or attacks" → two triggered abilities (per Sentinel of the Nameless City),
 *   sharing one composite body.
 * - The body gathers the top card, exiles it, then splits land vs. non-land
 *   ([FilterCollectionEffect]). A land leg (gated by [Conditions.CollectionContainsMatch])
 *   creates an Ox token and leaves the land sitting in exile. The non-land leg grants a
 *   may-play window until the end of the controller's next turn (impulse, mirroring Gila
 *   Courser / Alania's Pathmaker). Oracle says "cast"; the engine's may-play permission
 *   only lets you cast a nonland card anyway, so the window is faithful.
 */
private const val OX_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/c/e/cee3ecef-4566-4164-af39-89cb0bbbffeb.jpg?1712316060"

private val bruseTarlBody = Effects.Composite(
    listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
            storeAs = "exiledCard",
        ),
        MoveCollectionEffect(
            from = "exiledCard",
            destination = CardDestination.ToZone(Zone.EXILE),
        ),
        // Split the exiled card into land vs. non-land.
        FilterCollectionEffect(
            from = "exiledCard",
            filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
            storeMatching = "landCards",
            storeNonMatching = "nonLandCards",
        ),
        // Land: create a 2/2 white Ox token (the land stays in exile, unused).
        ConditionalEffect(
            condition = Conditions.CollectionContainsMatch("landCards", GameObjectFilter.Land),
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Ox"),
                imageUri = OX_TOKEN_IMAGE,
            ),
        ),
        // Otherwise: you may cast the non-land card until the end of your next turn.
        GrantMayPlayFromExileEffect("nonLandCards", MayPlayExpiry.UntilEndOfNextTurn),
    )
)

val BruseTarlRovingRancher = card("Bruse Tarl, Roving Rancher") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "Oxen you control have double strike.\n" +
        "Whenever Bruse Tarl enters or attacks, exile the top card of your library. If it's " +
        "a land card, create a 2/2 white Ox creature token. Otherwise, you may cast it until " +
        "the end of your next turn."

    // Oxen you control have double strike.
    staticAbility {
        ability = GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Ox").youControl()),
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = bruseTarlBody
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = bruseTarlBody
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Forrest Imel"
        flavorText = "\"I find a world where the herds finally listen to me, but now the " +
            "plants have an attitude?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/286c55c2-dcc1-4e87-a83f-9981d28ab62d.jpg?1712356070"
    }
}
