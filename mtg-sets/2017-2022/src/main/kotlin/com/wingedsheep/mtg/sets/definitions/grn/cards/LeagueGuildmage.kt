package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * League Guildmage
 * {U}{R}
 * Creature — Human Wizard
 * 2/2
 * {3}{U}, {T}: Draw a card.
 * {X}{R}, {T}: Copy target instant or sorcery spell you control with mana value X. You may choose
 * new targets for the copy.
 */
val LeagueGuildmage = card("League Guildmage") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    oracleText = "{3}{U}, {T}: Draw a card.\n" +
        "{X}{R}, {T}: Copy target instant or sorcery spell you control with mana value X. " +
        "You may choose new targets for the copy."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}"), Costs.Tap)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{R}"), Costs.Tap)
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl().manaValueEqualsX())
        effect = Effects.CopyTargetSpell(target = spell)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7439118d-4869-486e-8930-6e00db2634b2.jpg?1783934129"
    }
}
