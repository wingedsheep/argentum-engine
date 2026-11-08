package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scholar of Combustion — Foundations Jumpstart #19
 * {3}{R} · Creature — Human Wizard · 3/2
 *
 * When this creature enters, exile up to one target instant or sorcery card from your graveyard.
 * You may cast that card until the end of your next turn.
 *
 * The Practiced Scrollsmith gather → exile → grant pipeline with an optional target. The card is
 * an instant or sorcery, so the granted "play" permission is exactly "cast"; its normal timing and
 * costs still apply. [MayPlayExpiry.UntilEndOfNextTurn] keeps the permission through the rest of
 * this turn and the whole of your next one. Choosing no target leaves the gather empty, so the
 * ability does nothing.
 */
val ScholarOfCombustion = card("Scholar of Combustion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, exile up to one target instant or sorcery card from " +
        "your graveyard. You may cast that card until the end of your next turn. (You still pay " +
        "its costs. Timing rules still apply.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.InstantOrSorceryInYourGraveyard, optional = true)
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.ChosenTargets)
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard, MayPlayExpiry.UntilEndOfNextTurn))
        }
        description = "When this creature enters, exile up to one target instant or sorcery card " +
            "from your graveyard. You may cast that card until the end of your next turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Nereida"
        flavorText = "\"If the fire is silent, I'm technically not breaking the library rules!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23660e44-8546-438d-a2c4-e1cef6e50855.jpg?1783908864"

        ruling(
            "2024-11-08",
            "You pay all costs and follow all timing rules for the instant or sorcery card cast with " +
                "the permission granted by Scholar of Combustion's ability. For example, if the exiled " +
                "card is a sorcery, you may cast it only during your main phase while the stack is empty."
        )
    }
}
