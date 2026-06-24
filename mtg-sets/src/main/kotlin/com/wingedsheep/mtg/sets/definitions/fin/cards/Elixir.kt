package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Elixir
 * {1}
 * Artifact
 *
 * This artifact enters tapped.
 * {5}, {T}, Exile this artifact: Shuffle all nonland cards from your graveyard into your library.
 * You gain life equal to the number of cards shuffled into your library this way.
 *
 * Gathers only the nonland cards from the controller's graveyard, shuffles them into the library,
 * then gains life equal to the count actually moved (`shuffled_count`). Lands stay in the graveyard.
 */
val Elixir = card("Elixir") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{5}, {T}, Exile this artifact: Shuffle all nonland cards from your graveyard into your library. You gain life equal to the number of cards shuffled into your library this way."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.ExileSelf)
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Nonland),
                    storeAs = "shuffled"
                ),
                MoveCollectionEffect(
                    from = "shuffled",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)
                ),
                Effects.GainLife(DynamicAmount.VariableReference("shuffled_count"), EffectTarget.Controller)
            )
        )
        description = "{5}, {T}, Exile this artifact: Shuffle all nonland cards from your graveyard into your library. You gain life equal to the number of cards shuffled into your library this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Takeuchi Moto"
        flavorText = "Fully recovers HP and MP."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4b05d37-df62-475c-8371-735ed2fa1b05.jpg?1748706750"
    }
}
