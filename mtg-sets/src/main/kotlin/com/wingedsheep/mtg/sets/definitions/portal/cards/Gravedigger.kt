package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCardInGraveyard

/**
 * Gravedigger
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * When Gravedigger enters the battlefield, you may return target creature card
 * from your graveyard to your hand.
 */
val Gravedigger = card("Gravedigger") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnEnterBattlefield()
        optional = true
        target = TargetCardInGraveyard(filter = TargetFilter.CreatureInYourGraveyard)
        effect = ReturnFromGraveyardEffect(
            destination = SearchDestination.HAND
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Dermot Power"
        flavorText = "\"A grave is merely a grave if you disturb it only once.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b979d70e-d514-420f-886c-f60e2bb1861f.jpg"
    }
}
