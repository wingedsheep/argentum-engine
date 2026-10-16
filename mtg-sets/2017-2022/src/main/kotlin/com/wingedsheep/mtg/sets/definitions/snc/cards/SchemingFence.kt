package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Scheming Fence — Streets of New Capenna #219.
 *
 * The single-permanent sibling of Drana and Linvala: an optional "as this enters" pick of any
 * nonland permanent, read back by [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ChosenBySource]
 * to lock that permanent's activated abilities and to copy them (mana abilities included, loyalty
 * abilities not) onto the Fence, where `spendAsThoughAnyColor` relaxes the copies' colour
 * requirements — only "those abilities", not ones an Equipment or Aura grants the Fence. If the
 * chosen permanent leaves the battlefield, nothing is locked or gained any more.
 */
val SchemingFence = card("Scheming Fence") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 3
    oracleText = "As this creature enters, you may choose a nonland permanent.\n" +
        "Activated abilities of the chosen permanent can't be activated.\n" +
        "This creature has all activated abilities of the chosen permanent except for loyalty abilities. " +
        "You may spend mana as though it were mana of any color to activate those abilities."

    replacementEffect(
        EntersWithChoice(
            ChoiceType.CREATURE_ON_BATTLEFIELD,
            permanentFilter = GameObjectFilter.NonlandPermanent,
            mayChooseNothing = true
        )
    )

    staticAbility {
        ability = PreventActivatedAbilities(filter = GameObjectFilter.ChosenBySource)
    }
    staticAbility {
        ability = GainActivatedAbilitiesOfPermanents(
            grantedTo = GroupFilter.source(),
            sourceFilter = GameObjectFilter.ChosenBySource,
            includeManaAbilities = true,
            includeLoyaltyAbilities = false,
            spendAsThoughAnyColor = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af7ef030-7bae-4820-b51a-8fad57a8ff2f.jpg?1783923071"
    }
}
