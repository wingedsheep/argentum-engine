package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Memorial Vault
 * {3}{R}
 * Artifact
 * {T}, Sacrifice another artifact: Exile the top X cards of your library, where X is
 * one plus the mana value of the sacrificed artifact. You may play those cards this turn.
 */
val MemorialVault = card("Memorial Vault") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice another artifact: Exile the top X cards of your library, " +
        "where X is one plus the mana value of the sacrificed artifact. You may play those cards this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.Artifact))
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    DynamicAmount.Add(
                        DynamicAmount.Fixed(1),
                        DynamicAmount.EntityProperty(
                            EntityReference.Sacrificed(),
                            EntityNumericProperty.ManaValue
                        )
                    )
                ),
                storeAs = "exiledCards"
            ),
            MoveCollectionEffect(
                from = "exiledCards",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            Effects.GrantMayPlayFromExile("exiledCards", MayPlayExpiry.EndOfTurn)
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Javier Charro"
        flavorText = "Kavaron cannot be saved, but its culture can."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a14ecd13-325a-4555-b1dd-d0d0d0826031.jpg?1752947139"
        ruling("2025-07-25", "You pay all costs and follow all timing rules for cards played with the permission granted by Memorial Vault's ability. For example, if an exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2025-07-25", "If an artifact has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
