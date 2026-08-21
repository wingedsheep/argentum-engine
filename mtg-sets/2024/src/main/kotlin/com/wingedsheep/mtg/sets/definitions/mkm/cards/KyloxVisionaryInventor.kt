package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kylox, Visionary Inventor — Murders at Karlov Manor #214
 * {5}{U}{R} · Legendary Creature — Lizard Artificer · 4/4
 *
 * Menace, ward {2}, haste
 * Whenever Kylox attacks, sacrifice any number of other creatures, then exile the top X cards of
 * your library, where X is their total power. You may cast any number of instant and/or sorcery
 * spells from among the exiled cards without paying their mana costs.
 *
 * The attack trigger is the Villainous Wealth pipeline with a sacrifice bolted to the front, and
 * the whole card turns on the seam between the two: the sacrifice has to hand its victims'
 * characteristics to the step that counts them, and by then they are all in the graveyard.
 * [Effects.SacrificeAnyNumber] already captures a last-known-information snapshot per sacrificed
 * permanent (Rule 608.2h) and `CompositeEffect` merges those into the resolving context, so
 * [DynamicAmount.TotalPowerSacrificedThisWay] can sum them afterwards — which is exactly what the
 * 2024-02-02 ruling demands: "Use the power of the sacrificed creatures as they last existed on
 * the battlefield."
 *
 * `excludeSource` is load-bearing, not decoration: without it Kylox is a legal choice for its own
 * "any number of other creatures" and could be sacrificed mid-attack.
 *
 * Sacrificing nothing is a legal choice ("any number" includes zero), and X is then 0 — the gather
 * exiles nothing and the cast step finds an empty collection. Nothing special-cases that; every
 * step in the pipeline is already a no-op on an empty list.
 *
 * The cast step is [Effects.CastAnyNumberFromCollectionWithoutPayingCost], which casts during this
 * ability's own resolution — the controller can't bank the exiled cards for later, and timing
 * restrictions on the sorceries are ignored, both per the same ruling. Cards left uncast stay in
 * exile.
 */
val KyloxVisionaryInventor = card("Kylox, Visionary Inventor") {
    manaCost = "{5}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Lizard Artificer"
    oracleText = "Menace, ward {2}, haste\n" +
        "Whenever Kylox attacks, sacrifice any number of other creatures, then exile the top X " +
        "cards of your library, where X is their total power. You may cast any number of instant " +
        "and/or sorcery spells from among the exiled cards without paying their mana costs."
    power = 4
    toughness = 4
    keywords(Keyword.MENACE, Keyword.HASTE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                Effects.SacrificeAnyNumber(GameObjectFilter.Creature, excludeSource = true),
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        DynamicAmount.TotalPowerSacrificedThisWay,
                        player = Player.You
                    ),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player = Player.You)
                ),
                FilterCollectionEffect(
                    from = "exiled",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.InstantOrSorcery),
                    storeMatching = "castable"
                ),
                Effects.CastAnyNumberFromCollectionWithoutPayingCost("castable")
            )
        )
        description = "Whenever Kylox attacks, sacrifice any number of other creatures, then " +
            "exile the top X cards of your library, where X is their total power. You may cast " +
            "any number of instant and/or sorcery spells from among the exiled cards without " +
            "paying their mana costs."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00faa272-91ad-407b-9175-8fa1d02585b8.jpg?1783912845"

        ruling(
            "2024-02-02",
            "Use the power of the sacrificed creatures as they last existed on the battlefield to " +
                "determine the value of X."
        )
        ruling(
            "2024-02-02",
            "You choose which spells to cast (if any) as Kylox, Visionary Inventor's last ability " +
                "resolves. If you choose to cast any spells, you do so as part of the resolution " +
                "of that ability. You can't wait to cast them later in the turn. Timing " +
                "restrictions based on the cards' types are ignored."
        )
        ruling(
            "2024-02-02",
            "If you cast a spell \"without paying its mana cost\", you can't choose to cast it for " +
                "any alternative costs. You can, however, pay additional costs, such as kicker " +
                "costs. If the card has any mandatory additional costs, those must be paid to " +
                "cast the spell."
        )
        ruling(
            "2024-02-02",
            "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X " +
                "when casting it without paying its mana cost."
        )
    }
}
