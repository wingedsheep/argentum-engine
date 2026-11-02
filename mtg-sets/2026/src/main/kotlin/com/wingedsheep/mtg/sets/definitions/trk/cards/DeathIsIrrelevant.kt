package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Death Is Irrelevant
 * {6}{B}{B}
 * Enchantment
 *
 * Artifact creatures you control have indestructible.
 * At the beginning of your end step, assimilate target creature card from a graveyard. (Put it onto
 * the battlefield under your control with a +1/+1 counter. It's a Borg artifact creature and loses
 * all other creature types.)
 *
 * - The assimilated card is an artifact creature as it enters, so the first static covers it at once.
 */
val DeathIsIrrelevant = card("Death Is Irrelevant") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Artifact creatures you control have indestructible.\n" +
        "At the beginning of your end step, assimilate target creature card from a graveyard. " +
        "(Put it onto the battlefield under your control with a +1/+1 counter. It's a Borg artifact " +
        "creature and loses all other creature types.)"

    staticAbility {
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val card = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Assimilate(card, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "102"
        artist = "Narendra Bintara Adi"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/408cb5dd-1a79-47a9-98ea-41f3f4502c5e.jpg?1792668404"
    }
}
