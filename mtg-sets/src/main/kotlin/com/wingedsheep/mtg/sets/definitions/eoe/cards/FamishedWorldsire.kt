package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDevour
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.Effects

/**
 * Famished Worldsire
 * {5}{G}{G}{G}
 * Creature — Leviathan
 * 0/0
 *
 * Ward {3}
 * Devour land 3 (As this creature enters, you may sacrifice any number of lands.
 * It enters with three times that many +1/+1 counters on it.)
 * When this creature enters, look at the top X cards of your library, where X is
 * this creature's power. Put any number of land cards from among them onto the
 * battlefield tapped, then shuffle.
 */
val FamishedWorldsire = card("Famished Worldsire") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Leviathan"
    power = 0
    toughness = 0
    oracleText = "Ward {3}\n" +
        "Devour land 3 (As this creature enters, you may sacrifice any number of lands. " +
        "It enters with three times that many +1/+1 counters on it.)\n" +
        "When this creature enters, look at the top X cards of your library, where X is " +
        "this creature's power. Put any number of land cards from among them onto the " +
        "battlefield tapped, then shuffle."

    keywords(Keyword.WARD, Keyword.DEVOUR)
    keywordAbility(KeywordAbility.ward("{3}"))
    keywordAbility(KeywordAbility.devourLand(3))

    replacementEffect(
        EntersWithDevour(
            multiplier = 3,
            sacrificeFilter = GameObjectFilter.Land,
            variant = "land",
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = run {
            val countSource = DynamicAmounts.sourcePower()
            Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(countSource),
                        storeAs = "looked"
                    ),
                    SelectFromCollectionEffect(
                        from = "looked",
                        selection = SelectionMode.ChooseUpTo(countSource),
                        filter = GameObjectFilter.Land,
                        storeSelected = "toBattlefield",
                        storeRemainder = "rest"
                    ),
                    MoveCollectionEffect(
                        from = "toBattlefield",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
                    ),
                    MoveCollectionEffect(
                        from = "rest",
                        destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled)
                    )
                )
            )
        }
        description = "When this creature enters, look at the top X cards of your library, " +
            "where X is this creature's power. Put any number of land cards from among them " +
            "onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "182"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2934c9c8-d23a-462b-83d5-94e88c8663ac.jpg?1752947299"
        ruling("2025-07-25", "The value of X is calculated only once, as Famished Worldsire's last ability resolves.")
        ruling("2025-07-25", "Devour land is a variant of the devour ability. It allows you to sacrifice lands rather than creatures, but otherwise functions identically to devour.")
        ruling("2025-07-25", "You may choose not to sacrifice any lands for the devour land ability.")
    }
}
