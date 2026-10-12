package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Slagstone Refinery
 * {4}
 * Artifact
 * Whenever this artifact or another nontoken artifact you control is put into a graveyard from the
 * battlefield or is put into exile from the battlefield, create a tapped Powerstone token.
 *
 * "This artifact" and "another nontoken artifact you control" are split into two triggers so the
 * Refinery still sees its own departure even when it is itself a token (a token copy of it); the
 * subjects are disjoint, so no event fires both.
 */
val SlagstoneRefinery = card("Slagstone Refinery") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever this artifact or another nontoken artifact you control is put into a graveyard from the battlefield or is put into exile from the battlefield, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    val otherNontokenArtifact = GameObjectFilter.Artifact.nontoken().youControl()
    triggeredAbility {
        trigger = Triggers.or(Triggers.self.dies(), Triggers.self.leaves(to = Zone.EXILE))
        effect = Effects.CreatePowerstone(1, tapped = true)
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.another(otherNontokenArtifact).dies(),
            Triggers.another(otherNontokenArtifact).leaves(to = Zone.EXILE)
        )
        effect = Effects.CreatePowerstone(1, tapped = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7dbfa72-2025-455c-8cc4-afbf6c8e141a.jpg?1783920016"
        ruling("2022-10-14", "If Slagstone Refinery is put into a graveyard from the battlefield or exiled from the battlefield at the same time that one or more other nontoken artifacts controlled by the same player are put into a graveyard from the battlefield and/or exiled from the battlefield, its triggered ability will trigger once for each of them plus once for itself.")
        ruling("2022-10-14", "Although all the cards in The Brothers' War that create Powerstone tokens create a tapped Powerstone token, entering the battlefield tapped isn't part of the token's definition. Notably, if you create a token that is a copy of a Powerstone token, the token copy won't enter the battlefield tapped.")
    }
}
