package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Pippin, Guard of the Citadel
 * {W}{U}
 * Legendary Creature — Halfling Soldier
 * 2/2
 *
 * Vigilance, ward {1}
 * {T}: Another target creature you control gains protection from the card type of your choice
 * until end of turn.
 *
 * The activated ability uses [Effects.GrantProtectionFromChosenCardType], the card-type analogue
 * of the chosen-color protection effect: at resolution the controller picks a card type and the
 * target gains a floating `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword.
 */
val PippinGuardOfTheCitadel = card("Pippin, Guard of the Citadel") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Halfling Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance, ward {1}\n" +
        "{T}: Another target creature you control gains protection from the card type of your " +
        "choice until end of turn. (It can't be blocked, targeted, dealt damage, enchanted, or " +
        "equipped by anything of that type.)"

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.ward("{1}"))

    activatedAbility {
        cost = Costs.Tap
        target = Targets.OtherCreatureYouControl
        effect = Effects.GrantProtectionFromChosenCardType()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        flavorText = "\"Here do I swear fealty to Gondor, until my lord release me, or death take me.\""
        artist = "Bartłomiej Gaweł"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08b7a4d8-1183-430e-8ea4-016844f33200.jpg?1686969929"
    }
}
