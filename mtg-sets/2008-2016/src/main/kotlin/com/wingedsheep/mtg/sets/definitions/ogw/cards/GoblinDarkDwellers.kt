package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goblin Dark-Dwellers
 * {3}{R}{R}
 * Creature — Goblin
 * 4/4
 *
 * Menace
 * When this creature enters, you may cast target instant or sorcery card with mana value 3 or less
 * from your graveyard without paying its mana cost. If that spell would be put into your graveyard,
 * exile it instead.
 *
 * The card is targeted when the ability goes on the stack and cast during resolution, so declining
 * (or being unable to cast it) leaves it in the graveyard — the cast is the "may", not a prior move
 * to exile. The exile rider is the cast-this-way destination ([AfterResolveDestination.EXILE]).
 */
val GoblinDarkDwellers = card("Goblin Dark-Dwellers") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 4
    toughness = 4
    oracleText = "Menace\n" +
        "When this creature enters, you may cast target instant or sorcery card with mana value 3 or " +
        "less from your graveyard without paying its mana cost. If that spell would be put into your " +
        "graveyard, exile it instead."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(
            TargetFilter(
                GameObjectFilter.InstantOrSorcery.manaValueAtMost(3).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(
                Effects.May(
                    Effects.CastFromCollectionWithoutPayingCost(
                        card,
                        insteadOfGraveyard = AfterResolveDestination.EXILE,
                    ),
                    descriptionOverride = "You may cast target instant or sorcery card with mana value 3 or " +
                        "less from your graveyard without paying its mana cost.",
                )
            )
        }
        description = "When this creature enters, you may cast target instant or sorcery card with mana " +
            "value 3 or less from your graveyard without paying its mana cost. If that spell would be put " +
            "into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddf91a4e-97f7-4189-823d-b619bb3b3d55.jpg?1783937906"
        ruling(
            "2016-01-22",
            "You cast the card during the resolution of the enters-the-battlefield ability, not later in the turn. If it's a sorcery card, ignore the timing restrictions based on it being a sorcery card. Other timing restrictions, such as \"Cast [this card] only during combat,\" must be followed."
        )
        ruling(
            "2016-01-22",
            "If you can't cast the target instant or sorcery card, perhaps because there are no legal targets available, or if you choose not to cast it, it will remain in your graveyard."
        )
        ruling(
            "2016-01-22",
            "If you cast a card \"without paying its mana cost,\" you can't pay alternative costs such as surge costs. You can pay additional costs. If the card has mandatory additional costs, you must pay those."
        )
        ruling(
            "2016-01-22",
            "If the instant or sorcery card has {X} in its mana cost, you must choose 0 as its value."
        )
        ruling(
            "2016-01-22",
            "If an instant or sorcery card you cast this way is countered, it will be exiled."
        )
        ruling(
            "2016-01-22",
            "If an instant or sorcery card you cast this way goes to a zone other than exile or a graveyard, perhaps because one of its abilities says to put it into its owner's hand, it won't be exiled. This is true even if the card would be put into a graveyard later that turn."
        )
    }
}
