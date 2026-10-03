package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Taranika, Akroan Veteran
 * {1}{W}{W}
 * Legendary Creature — Human Soldier
 * 3/3
 * Vigilance
 * Whenever Taranika attacks, untap another target creature you control. Until end of turn, that
 * creature has base power and toughness 4/4 and gains indestructible.
 *
 * An attack trigger that untaps the target, then Water Wings' shape:
 * [Effects.SetBasePowerAndToughness] (layer 7b, end of turn) plus an end-of-turn
 * [Effects.GrantKeyword] for indestructible.
 */
val TaranikaAkroanVeteran = card("Taranika, Akroan Veteran") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever Taranika attacks, untap another target creature you control. Until end of turn, " +
        "that creature has base power and toughness 4/4 and gains indestructible."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Untap(t) then
            (Effects.SetBasePowerAndToughness(4, 4, t) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Eric Deschamps"
        flavorText = "\"I like to think Kytheon keeps watch over all of us.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8edd707-5ba3-4978-9a0b-efd0cda367c1.jpg?1783931589"
    }
}
