package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cori Mountain Monastery
 * Land
 *
 * This land enters tapped unless you control a Plains or an Island.
 * {T}: Add {R}.
 * {3}{R}, {T}: Exile the top card of your library. Until the end of your next turn, you may
 * play that card.
 */
val CoriMountainMonastery = card("Cori Mountain Monastery") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped unless you control a Plains or an Island.\n" +
        "{T}: Add {R}.\n" +
        "{3}{R}, {T}: Exile the top card of your library. Until the end of your next turn, you " +
        "may play that card."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.Any(
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains")),
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))
            )
        )
    )

    // {T}: Add {R}.
    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {3}{R}, {T}: Exile the top card of your library. Until the end of your next turn, you may
    // play that card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}"), Costs.Tap)
        effect = Effects.Pipeline {
            val exiledCard = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                name = "exiledCard"
            )
            move(
                exiledCard,
                destination = CardDestination.ToZone(Zone.EXILE)
            )
            run(GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Arthur Yuan"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9312821a-2059-4f44-9b20-c9522b827e38.jpg?1743204997"
    }
}
