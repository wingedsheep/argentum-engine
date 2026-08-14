package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Night Nurse, Healer of Heroes — Marvel Super Heroes #26
 * {1}{W} · Legendary Creature — Human Doctor Hero · 2/1
 *
 * Flash
 * Lifelink
 * When Night Nurse enters, choose target permanent card in your graveyard that was put there
 * from anywhere this turn. Return it to your hand.
 *
 * Modeling notes:
 *  - "that was put there from anywhere this turn" is the zone-agnostic
 *    `StatePredicate.PutIntoGraveyardThisTurn` (`.putIntoGraveyardThisTurn()`), the same predicate
 *    FDN's Abyssal Harvester uses — a card milled, discarded, countered, or sacrificed into the
 *    graveyard qualifies just as much as one that died. Its zone-restricted sibling
 *    (`putIntoGraveyardFromBattlefieldThisTurn`) would wrongly exclude those.
 *  - "in your graveyard" is `.ownedByYou()` on a [Zone.GRAVEYARD]-scoped [TargetFilter]
 *    (Elvish Regrower's shape); graveyard membership follows ownership.
 *  - The trigger is mandatory — "choose target … Return it" — so no `optional`. With no legal
 *    target the ability is simply removed from the stack (CR 608.2b).
 */
val NightNurseHealerOfHeroes = card("Night Nurse, Healer of Heroes") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Doctor Hero"
    power = 2
    toughness = 1
    oracleText = "Flash\nLifelink\n" +
        "When Night Nurse enters, choose target permanent card in your graveyard that was put " +
        "there from anywhere this turn. Return it to your hand."

    keywords(Keyword.FLASH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val rescued = target(
            "permanent card in your graveyard that was put there this turn",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.ownedByYou().putIntoGraveyardThisTurn(),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.Move(rescued, Zone.HAND)
        description = "When Night Nurse enters, choose target permanent card in your graveyard " +
            "that was put there from anywhere this turn. Return it to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Gal Or"
        flavorText = "For some, making a better world means mending bodies while pretending to never see faces."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3199907b-9f24-4554-942a-8ab5a7701717.jpg?1783902972"
    }
}
