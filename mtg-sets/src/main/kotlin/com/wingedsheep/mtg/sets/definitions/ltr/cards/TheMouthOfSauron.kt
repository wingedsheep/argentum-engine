package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * The Mouth of Sauron
 * {3}{U}{B}
 * Legendary Creature — Human Advisor
 * 3/4
 *
 * When The Mouth of Sauron enters, target player mills three cards. Then
 * amass Orcs X, where X is the number of instant and sorcery cards in that
 * player's graveyard.
 *
 * X is calculated after the mill resolves, so the just-milled cards count if
 * they are instant or sorcery cards.
 */
val TheMouthOfSauron = card("The Mouth of Sauron") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 4
    oracleText = "When The Mouth of Sauron enters, target player mills three cards. Then amass Orcs X, " +
        "where X is the number of instant and sorcery cards in that player's graveyard. " +
        "(Put X +1/+1 counters on an Army you control. It's also an Orc. If you don't control an Army, " +
        "create a 0/0 black Orc Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target player", Targets.Player)
        effect = Effects.Composite(
            LibraryPatterns.mill(3, EffectTarget.ContextTarget(0)),
            Effects.Amass(
                DynamicAmount.Count(
                    player = Player.ContextPlayer(0),
                    zone = Zone.GRAVEYARD,
                    filter = GameObjectFilter.InstantOrSorcery
                ),
                "Orc"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76a88814-aa30-4297-b338-3d851bfe7256.jpg?1686969905"
    }
}
