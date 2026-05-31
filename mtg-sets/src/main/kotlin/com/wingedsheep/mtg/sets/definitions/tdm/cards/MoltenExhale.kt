package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect

/**
 * Molten Exhale
 * {1}{R}
 * Sorcery
 *
 * You may cast this spell as though it had flash if you behold a Dragon as an
 * additional cost to cast it. (To behold a Dragon, choose a Dragon you control or
 * reveal a Dragon card from your hand.)
 * Molten Exhale deals 4 damage to target creature or planeswalker.
 *
 * The behold-grants-flash clause is modelled with [KeywordAbility.flashKicker] over a
 * non-mana [AdditionalCost.Behold]: paying the optional behold cost unlocks instant-speed
 * casting without branching the effect (the 4 damage is unconditional).
 */
val MoltenExhale = card("Molten Exhale") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if you behold a Dragon as an " +
        "additional cost to cast it. (To behold a Dragon, choose a Dragon you control or " +
        "reveal a Dragon card from your hand.)\n" +
        "Molten Exhale deals 4 damage to target creature or planeswalker."

    keywordAbility(KeywordAbility.flashKicker(AdditionalCost.Behold(filter = Filters.WithSubtype("Dragon"))))

    spell {
        val t = target("target", Targets.CreatureOrPlaneswalker)
        effect = DealDamageEffect(4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ab95aab-a4bf-4131-83a0-2c138b6f20c3.jpg?1743204414"
    }
}
