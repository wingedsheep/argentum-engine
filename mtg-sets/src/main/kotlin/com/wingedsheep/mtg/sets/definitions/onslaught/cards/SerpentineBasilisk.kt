package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAtEndOfCombatEffect
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Serpentine Basilisk
 * {2}{G}{G}
 * Creature — Basilisk
 * 2/3
 * Whenever Serpentine Basilisk deals combat damage to a creature,
 * destroy that creature at end of combat.
 * Morph {1}{G}{G}
 */
val SerpentineBasilisk = card("Serpentine Basilisk") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Basilisk"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToCreature
        effect = DestroyAtEndOfCombatEffect(EffectTarget.TriggeringEntity)
    }

    morph = "{1}{G}{G}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "280"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/large/front/4/0/4052a5af-20b2-4817-8c94-78d488ee220f.jpg?1562936568"
    }
}
