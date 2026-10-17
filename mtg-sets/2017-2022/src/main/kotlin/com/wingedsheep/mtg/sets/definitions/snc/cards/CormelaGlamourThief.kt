package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cormela, Glamour Thief
 * {1}{U}{B}{R}
 * Legendary Creature — Vampire Rogue
 * 2/4
 * Haste
 * {1}, {T}: Add {U}{B}{R}. Spend this mana only to cast instant and/or sorcery spells.
 * When Cormela dies, return up to one target instant or sorcery card from your graveyard to your hand.
 *
 * The mana ability is the Darigaaz's Attendant three-colour add, each mana carrying
 * [ManaRestriction.InstantOrSorceryOnly] (the Vodalian Arcanist restriction).
 */
val CormelaGlamourThief = card("Cormela, Glamour Thief") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "BRU"
    typeLine = "Legendary Creature — Vampire Rogue"
    oracleText = "Haste\n" +
        "{1}, {T}: Add {U}{B}{R}. Spend this mana only to cast instant and/or sorcery spells.\n" +
        "When Cormela dies, return up to one target instant or sorcery card from your graveyard to your hand."
    power = 2
    toughness = 4

    keywords(Keyword.HASTE)

    activatedAbility {
        val spells = ManaRestriction.InstantOrSorceryOnly
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE, restriction = spells) then
            Effects.AddMana(Color.BLACK, restriction = spells) then
            Effects.AddMana(Color.RED, restriction = spells)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter.InstantOrSorceryInYourGraveyard, optional = true)
        effect = Effects.ReturnToHand(card)
        description = "When Cormela dies, return up to one target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Bram Sels"
        flavorText = "She sees the world as a gift meant for her."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/221ac34a-94ed-4c49-afad-b19cac541731.jpg?1783923089"
    }
}
