package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jack-o'-Lantern
 * {1}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Exile up to one target card from a graveyard. Draw a card.
 * {1}, Exile this card from your graveyard: Add one mana of any color.
 *
 * The second ability is a mana ability that functions only from the graveyard (CR 113.6m; still a
 * mana ability, CR 605.1a).
 */
val JackOLantern = card("Jack-o'-Lantern") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Exile up to one target card from a graveyard. Draw a card.\n" +
        "{1}, Exile this card from your graveyard: Add one mana of any color."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val graveyardCard = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Exile(graveyardCard) then Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.ExileSelf)
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
        activateFromZone = Zone.GRAVEYARD
        description = "{1}, Exile this card from your graveyard: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Josu Hernaiz"
        flavorText = "By the end of the festival, it was the only thing still grinning."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21b589ab-45a0-480a-a891-581c34f8a9bf.jpg?1783925548"
    }
}
