package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eerie Soultender — Dominaria United #92
 * {2}{B}
 * Creature — Spirit Cleric
 * 3/1
 * When this creature enters, mill three cards.
 * {4}{B}, Exile this card from your graveyard: Return another target creature card from your
 * graveyard to your hand.
 */
val EerieSoultender = card("Eerie Soultender") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit Cleric"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, mill three cards. (Put the top three cards of your library into your graveyard.)\n" +
        "{4}{B}, Exile this card from your graveyard: Return another target creature card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        val creature = target(TargetFilter.CreatureInYourGraveyard.other())
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/335f3146-743c-4a25-86b5-c45e9c66eb15.jpg?1783921334"
    }
}
