package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Nexus of Becoming
 * {6}
 * Artifact
 *
 * At the beginning of combat on your turn, draw a card. Then you may exile an artifact
 * or creature card from your hand. If you do, create a token that's a copy of the exiled
 * card, except it's a 3/3 Golem artifact creature in addition to its other types.
 *
 * Modeling: the optional exile is a Gather → Select(ChooseUpTo 1) → Move-to-exile pipeline
 * over hand artifact/creature cards. The token copies the *exiled* card (CR 707: copiable
 * values are the card's printed characteristics), referenced via [EffectTarget.PipelineTarget]
 * — the same shape Mardu Siegebreaker uses to copy a card it exiled. "3/3" overrides the
 * copy's P/T; "Golem artifact creature in addition to its other types" unions the Golem
 * subtype plus the ARTIFACT and CREATURE card types onto the copied type line.
 */
val NexusOfBecoming = card("Nexus of Becoming") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of combat on your turn, draw a card. Then you may exile an " +
        "artifact or creature card from your hand. If you do, create a token that's a copy of " +
        "the exiled card, except it's a 3/3 Golem artifact creature in addition to its other types."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.Composite(listOf(
            Effects.DrawCards(1),
            // You may exile an artifact or creature card from your hand.
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.You,
                    filter = GameObjectFilter.CreatureOrArtifact
                ),
                storeAs = "candidates"
            ),
            SelectFromCollectionEffect(
                from = "candidates",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "exiledCard",
                prompt = "You may exile an artifact or creature card from your hand"
            ),
            MoveCollectionEffect(
                from = "exiledCard",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            // If you did, create a 3/3 Golem artifact creature token copy of the exiled card.
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.PipelineTarget("exiledCard"),
                overridePower = 3,
                overrideToughness = 3,
                addedSubtypes = setOf(Subtype("Golem")),
                addCardTypes = setOf("ARTIFACT", "CREATURE")
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "25"
        artist = "Adam Volker"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0f61742-522c-4b36-97db-41d0c412a072.jpg?1739804233"
    }
}
