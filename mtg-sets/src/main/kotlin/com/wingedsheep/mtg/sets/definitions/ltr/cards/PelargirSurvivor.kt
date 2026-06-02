package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Pelargir Survivor
 * {1}{U}
 * Creature — Human Peasant
 * 1/3
 *
 * {T}: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell.
 * {5}{U}, {T}: Target player mills three cards.
 */
val PelargirSurvivor = card("Pelargir Survivor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Peasant"
    power = 1
    toughness = 3
    oracleText = "{T}: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell.\n" +
        "{5}{U}, {T}: Target player mills three cards. (They put the top three cards of their library into their graveyard.)"

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(1, ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{U}"), Costs.Tap)
        val player = target("target player", Targets.Player)
        effect = LibraryPatterns.mill(3, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Craig J Spearing"
        flavorText = "\"The Corsairs are upon us! Back to the walls! Back to the City before all are overwhelmed!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa59141a-4645-4316-b714-bbf2c139786e.jpg?1686968236"
    }
}
