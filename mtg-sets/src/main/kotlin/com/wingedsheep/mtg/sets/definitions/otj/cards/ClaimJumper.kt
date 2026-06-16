package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Claim Jumper
 * {2}{W}
 * Creature — Rabbit Mercenary
 * 3/3
 *
 * Vigilance
 * When this creature enters, if an opponent controls more lands than you, you may search your
 * library for a Plains card and put it onto the battlefield tapped. Then if an opponent controls
 * more lands than you, repeat this process once. If you search your library this way, shuffle.
 *
 * The enters ability is a [Triggers.EntersBattlefield] triggered ability with an intervening-if
 * ([CardBuilder] `triggerCondition`) of [Conditions.OpponentControlsMoreLands] — checked both as
 * the trigger goes on the stack and again as it resolves (CR 603.4). Each "process" is an optional
 * ([MayEffect]) `searchLibrary` for a Plains card straight onto the battlefield tapped, which
 * shuffles only when a search actually happens (declining the may means no search and no shuffle).
 * "Repeat this process once" is a single re-run gated by a fresh [ConditionalEffect] check of the
 * same land-count comparison, so the second search only occurs if an opponent still controls more
 * lands than you.
 */
val ClaimJumper = card("Claim Jumper") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Mercenary"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, if an opponent controls more lands than you, you may search " +
        "your library for a Plains card and put it onto the battlefield tapped. Then if an " +
        "opponent controls more lands than you, repeat this process once. If you search your " +
        "library this way, shuffle."

    keywords(Keyword.VIGILANCE)

    // One "process": optionally search for a Plains card and put it onto the battlefield tapped.
    val searchForPlains = MayEffect(
        Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.OpponentControlsMoreLands
        effect = Effects.Composite(
            listOf(
                searchForPlains,
                // "Then if an opponent controls more lands than you, repeat this process once."
                ConditionalEffect(
                    condition = Conditions.OpponentControlsMoreLands,
                    effect = searchForPlains
                )
            )
        )
        description = "When this creature enters, if an opponent controls more lands than you, you " +
            "may search your library for a Plains card and put it onto the battlefield tapped. Then " +
            "if an opponent controls more lands than you, repeat this process once. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/654ade6b-0369-4b90-a744-2f57a45b04f4.jpg?1712355252"

        ruling("2024-04-12", "The number of lands each player controls is checked as Claim Jumper's ability resolves and again before the process repeats.")
        ruling("2024-04-12", "If you don't search your library, you don't shuffle. You may also choose to search but find no card.")
    }
}
