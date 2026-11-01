package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dreadhorde Arcanist — War of the Spark #125 (canonical printing)
 * {1}{R} · Creature — Zombie Wizard · 1/3
 *
 * Trample
 * Whenever this creature attacks, you may cast target instant or sorcery card with mana value
 * less than or equal to this creature's power from your graveyard without paying its mana cost.
 * If that spell would be put into your graveyard, exile it instead.
 *
 * Efreet Flamepainter's resolution-time free cast with the exile-instead rider, on an attack
 * trigger. The mana-value cap rides on the target filter as `manaValueAtMostDynamic` over
 * [DynamicAmounts.sourcePower] (Gurgling Anointer's shape), so it is checked when the target is
 * chosen and again on resolution, falling back to last-known power if the Arcanist has left the
 * battlefield (2021-03-19 rulings).
 */
val DreadhordeArcanist = card("Dreadhorde Arcanist") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever this creature attacks, you may cast target instant or sorcery card with mana " +
        "value less than or equal to this creature's power from your graveyard without paying its " +
        "mana cost. If that spell would be put into your graveyard, exile it instead."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(
            TargetFilter(
                GameObjectFilter.InstantOrSorcery.ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.sourcePower()),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Pipeline {
            val spellCard = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(
                    from = spellCard,
                    insteadOfGraveyard = AfterResolveDestination.EXILE
                )
            ))
        }
        description = "Whenever this creature attacks, you may cast target instant or sorcery card " +
            "with mana value less than or equal to this creature's power from your graveyard " +
            "without paying its mana cost. If that spell would be put into your graveyard, exile " +
            "it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd97b3cf-924e-4f77-bb82-0bf19592389f.jpg?1783933429"
        ruling("2021-03-19", "The instant or sorcery card you target must have mana value less than or equal to Dreadhorde Arcanist's power immediately after Dreadhorde Arcanist has attacked. Any other abilities that trigger when creatures you control attack won't have resolved yet.")
        ruling("2021-03-19", "If Dreadhorde Arcanist leaves the battlefield before its triggered ability resolves, use its power as it last existed on the battlefield to check whether the card is still a legal target.")
        ruling("2021-03-19", "If you cast the card, you do so as part of the resolution of Dreadhorde Arcanist's triggered ability. You can't wait to cast it later in the turn. Timing permissions based on a card's type are ignored, and the spell will resolve before blockers are declared.")
        ruling("2021-03-19", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the card.")
        ruling("2021-03-19", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
