package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sharkey, Tyrant of the Shire
 * Legendary Creature — Avatar Rogue
 * {2}{U}{B}, 2/4
 *
 * Activated abilities of lands your opponents control can't be activated unless they're mana abilities.
 * Sharkey has all activated abilities of lands your opponents control except mana abilities.
 * Mana of any type can be spent to activate Sharkey's abilities.
 *
 * Modeled with three reusable static abilities:
 *  - [PreventActivatedAbilities] with `nonManaAbilitiesOnly = true` over `Land.opponentControls()`
 *    locks opponents' lands' non-mana abilities while leaving their mana abilities usable (piece 1).
 *  - [GainActivatedAbilitiesOfPermanents] grants Sharkey (Self) copies of the non-mana activated
 *    abilities of opponents' lands; the copies use Sharkey as their source (piece 2).
 *  - [SpendAnyManaTypeForActivatedAbilities] relaxes the colored requirements of Sharkey's own
 *    activated-ability mana costs to "any type" (piece 3).
 */
val SharkeyTyrantOfTheShire = card("Sharkey, Tyrant of the Shire") {
    typeLine = "Legendary Creature — Avatar Rogue"
    manaCost = "{2}{U}{B}"
    power = 2
    toughness = 4
    colorIdentity = "UB"
    oracleText =
        "Activated abilities of lands your opponents control can't be activated unless they're mana abilities.\n" +
        "Sharkey has all activated abilities of lands your opponents control except mana abilities.\n" +
        "Mana of any type can be spent to activate Sharkey's abilities."

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Land.opponentControls(),
            nonManaAbilitiesOnly = true
        )
    }
    staticAbility {
        ability = GainActivatedAbilitiesOfPermanents(
            grantedTo = GroupFilter.source(),
            sourceFilter = GameObjectFilter.Land.opponentControls(),
            includeManaAbilities = false
        )
    }
    staticAbility {
        ability = SpendAnyManaTypeForActivatedAbilities(filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e446bd-8295-4fca-957a-e4710a15d8e8.jpg?1686970050"
    }
}
