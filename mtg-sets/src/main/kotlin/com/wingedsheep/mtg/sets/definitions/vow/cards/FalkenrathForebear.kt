package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Falkenrath Forebear
 * {2}{B}
 * Creature — Vampire
 * 3/1
 *
 * Flying
 * This creature can't block.
 * Whenever this creature deals combat damage to a player, create a Blood token.
 * {B}, Sacrifice two Blood tokens: Return this card from your graveyard to the battlefield.
 *
 * The recursion ability activates from the graveyard (like Persistent Specimen):
 * [Effects.PutOntoBattlefield] on Self from [Zone.GRAVEYARD], its cost combining {B} with
 * sacrificing two Blood tokens ([Costs.SacrificeMultiple] over the Blood-artifact filter).
 */
val FalkenrathForebear = card("Falkenrath Forebear") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "This creature can't block.\n" +
        "Whenever this creature deals combat damage to a player, create a Blood token. (It's an " +
        "artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "{B}, Sacrifice two Blood tokens: Return this card from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock(filter = GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.CreateBlood(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Artifact.withSubtype("Blood")),
        )
        effect = Effects.PutOntoBattlefield(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        description = "{B}, Sacrifice two Blood tokens: Return this card from your graveyard to " +
            "the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "334"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48d43ab9-772d-4f4d-8536-3ea60e8f9f82.jpg?1782702954"
    }
}
