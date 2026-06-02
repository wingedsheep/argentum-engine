package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Rite of Renewal
 * {3}{G}
 * Sorcery
 * Return up to two target permanent cards from your graveyard to your hand.
 * Target player shuffles up to four target cards from their graveyard into their library.
 * Exile Rite of Renewal.
 *
 * Modeling note — both "up to N target ..." selections are modeled with the resolution-time
 * Gather → Select → Move pipeline rather than cast-time target requirements. The engine slices
 * a cast's flat target list to requirements by each requirement's *maximum* count, so two
 * "up to N" target groups in one spell can't be disambiguated when an earlier group is only
 * partially filled (see `EffectContext.buildNamedTargets` / `TargetValidator`). The pipeline
 * handles "up to N" cleanly and is the established pattern for graveyard selection
 * (Thundertrap Trainer, The Bath Song). Graveyard cards can't have hexproof/shroud, so there
 * is no targeting-protection fidelity loss from resolving these as pipeline selections.
 *
 * The shuffle clause follows the Gaea's Blessing precedent in this codebase: each selected
 * card is shuffled into its owner's library (the correct net result of "target player shuffles
 * ... into their library"); the engine has no cross-target "owned by the separately targeted
 * player" scoping, so the redundant standalone player target is omitted.
 */
val RiteOfRenewal = card("Rite of Renewal") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to two target permanent cards from your graveyard to your hand. " +
        "Target player shuffles up to four target cards from their graveyard into their library. " +
        "Exile Rite of Renewal."

    spell {
        effect = Effects.Composite(
            listOf(
                // Return up to two permanent cards from your graveyard to your hand.
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Permanent
                    ),
                    storeAs = "yourGraveyard"
                ),
                SelectFromCollectionEffect(
                    from = "yourGraveyard",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    storeSelected = "toHand",
                    selectedLabel = "Return to hand"
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                // Shuffle up to four cards from graveyards into their owners' libraries.
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.Each
                    ),
                    storeAs = "anyGraveyard"
                ),
                SelectFromCollectionEffect(
                    from = "anyGraveyard",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(4)),
                    storeSelected = "toLibrary",
                    selectedLabel = "Shuffle into library"
                ),
                MoveCollectionEffect(
                    from = "toLibrary",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled)
                )
            )
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Gaboleps"
        flavorText = "The greatest honor a Sultai can receive is to be returned to the realm of the living."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f737698a-d934-4851-b238-828959ef4835.jpg?1743204579"
    }
}
