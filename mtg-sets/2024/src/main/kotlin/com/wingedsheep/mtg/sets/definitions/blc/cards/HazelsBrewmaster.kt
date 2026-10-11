package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hazel's Brewmaster {3}{B}
 * Creature — Squirrel Warlock
 * 3/4
 *
 * Menace
 * Whenever this creature enters or attacks, exile up to one target card from a graveyard and
 * create a Food token.
 * Foods you control have all activated abilities of all creature cards exiled with this creature.
 *
 * The enters and attacks halves are two triggers sharing one body. The exile is linked to the
 * Brewmaster ([Effects.ExileLinkedToSource]), which fills the `LINKED_EXILE` donor pool read by
 * [HasAllActivatedAbilitiesOfCards] — narrowed to creature cards and granted to every Food you
 * control (Agatha's Soul Cauldron's shape with a Food receiver). Each Food is the granted
 * abilities' source, so `{T}` taps the Food and self-references bind to it.
 */
val HazelsBrewmaster = card("Hazel's Brewmaster") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Squirrel Warlock"
    power = 3
    toughness = 4
    oracleText = "Menace\n" +
        "Whenever this creature enters or attacks, exile up to one target card from a graveyard " +
        "and create a Food token.\n" +
        "Foods you control have all activated abilities of all creature cards exiled with this creature."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.ExileLinkedToSource(exiled) then Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val exiled = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.ExileLinkedToSource(exiled) then Effects.CreateFood()
    }

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.LINKED_EXILE,
            cardFilter = Filters.Creature,
            receivedBy = GroupFilter(GameObjectFilter.Artifact.withSubtype("Food").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52af8b70-a9c8-40d7-99da-fa51dc293688.jpg?1783910733"

        ruling(
            "2024-07-26",
            "You don't have to choose a target for Hazel's Brewmaster's second ability. However, if you do, " +
                "and that target is illegal as the ability tries to resolve, it won't resolve and none of its " +
                "effects will happen. You won't create a Food token."
        )
        ruling(
            "2024-07-26",
            "Hazel's Brewmaster's last ability grants only activated abilities. It doesn't grant keyword " +
                "abilities (unless those keyword abilities are activated), triggered abilities, or static abilities."
        )
        ruling(
            "2024-07-26",
            "The granted abilities effectively use \"this permanent\" rather than \"[that card's name],\" so you " +
                "treat the abilities as though they were printed on the permanent that gained the ability."
        )
    }
}
