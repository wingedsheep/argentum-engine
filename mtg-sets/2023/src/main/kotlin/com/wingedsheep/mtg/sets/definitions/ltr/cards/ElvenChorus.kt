package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Elven Chorus
 * {3}{G}
 * Enchantment
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 * Creatures you control have "{T}: Add one mana of any color."
 */
val ElvenChorus = card("Elven Chorus") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast creature spells from the top of your library.\n" +
        "Creatures you control have \"{T}: Add one mana of any color.\""

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Creature
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = AbilityCost.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Anato Finnstark"
        flavorText = "\"A Elbereth Gilthoniel, silivren penna míriel o menel aglar elenath!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/616cfb94-3faf-44fe-bf04-e90643765e48.jpg?1686969302"
    }
}
