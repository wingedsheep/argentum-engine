package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Firemane Avenger
 * {2}{R}{W}
 * Creature — Angel
 * 3/3
 * Flying
 * Battalion — Whenever this creature and at least two other creatures attack, this creature deals
 * 3 damage to any target and you gain 3 life.
 */
val FiremaneAvenger = card("Firemane Avenger") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\nBattalion — Whenever this creature and at least two other creatures attack, this " +
        "creature deals 3 damage to any target and you gain 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Wayne Reynolds"
        flavorText = "Firemane Nevena led the victorious assault on the Barbu Rooftop rebels. Now the Gateless " +
            "have marked her for death."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e244c198-efdc-492a-9c52-76aac006de9d.jpg?1783940108"
    }
}
