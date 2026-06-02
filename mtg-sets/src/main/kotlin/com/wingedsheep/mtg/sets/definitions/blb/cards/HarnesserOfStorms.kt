package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Harnesser of Storms {2}{R}
 * Creature — Otter Wizard
 * 1/4
 *
 * Whenever you cast a noncreature or Otter spell, you may exile the top card
 * of your library. Until end of turn, you may play that card.
 * This ability triggers only once each turn.
 */
val HarnesserOfStorms = card("Harnesser of Storms") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Otter Wizard"
    power = 1
    toughness = 4
    oracleText = "Whenever you cast a noncreature or Otter spell, you may exile the top card of your library. Until end of turn, you may play that card. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Noncreature or GameObjectFilter.Any.withSubtype(Subtype("Otter")),
        )
        oncePerTurn = true
        effect = MayEffect(
            Effects.Composite(listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "exiledCard"
                ),
                MoveCollectionEffect(
                    from = "exiledCard",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                GrantMayPlayFromExileEffect("exiledCard")
            ))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Bram Sels"
        flavorText = "She feeds and powers her entire village."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b56beeb6-88ca-475e-8654-1d4e8b4aa3c0.jpg?1721426630"
        ruling("2024-07-26", "You pay all costs and follow all timing rules for cards played this way. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
