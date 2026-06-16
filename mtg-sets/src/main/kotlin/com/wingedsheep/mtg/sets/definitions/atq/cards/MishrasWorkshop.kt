package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Mishra's Workshop
 * Land
 * {T}: Add {C}{C}{C}. Spend this mana only to cast artifact spells.
 */
val MishrasWorkshop = card("Mishra's Workshop") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}{C}{C}. Spend this mana only to cast artifact spells."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            3,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = false
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{C}{C}. Spend this mana only to cast artifact spells."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Kaja Foglio"
        flavorText = "Though he eventually came to despise Tocasia, Mishra listened well to her lessons on clarity of purpose. Unlike his brother, he focused his mind on a single goal."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/135de5c7-6ac9-4b68-8f1a-97f120a4b125.jpg?1745319958"
    }
}
