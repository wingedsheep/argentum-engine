package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elder Owyn Lyons — Fallout #103
 * {2}{W}{U} · Legendary Creature — Human Knight · 3/3
 *
 * Artifacts you control have ward {1}.
 * When Elder Owyn Lyons enters or dies, return target artifact card from your graveyard to your hand.
 *
 * The ward grant is a [GrantWard] lord over the artifacts you control; the engine synthesizes the
 * ward trigger for every permanent the filter matches. "Enters or dies" is two triggers sharing
 * one target and effect.
 */
val ElderOwynLyons = card("Elder Owyn Lyons") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Artifacts you control have ward {1}. (Whenever an artifact you control becomes the " +
        "target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "When Elder Owyn Lyons enters or dies, return target artifact card from your graveyard to your hand."

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{1}"),
            filter = GroupFilter(GameObjectFilter.Artifact.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.ArtifactInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
        description = "When Elder Owyn Lyons enters, return target artifact card from your graveyard to your hand."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(TargetFilter.ArtifactInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
        description = "When Elder Owyn Lyons dies, return target artifact card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d923c845-59e0-403e-b57b-955fec528f1e.jpg?1783912388"
    }
}
