package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dromoka, the Eternal — Fate Reforged #151 (canonical printing)
 * {3}{G}{W}
 * Legendary Creature — Dragon
 * 5/5
 * Flying
 * Whenever a Dragon you control attacks, bolster 2.
 *
 * A per-attacker ANY-bound attack trigger (one instance per attacking Dragon, Dromoka included).
 * Each instance picks its least-toughness creature as it resolves, so a creature bolstered by the
 * first instance may no longer be the smallest when the second resolves.
 */
val DromokaTheEternal = card("Dromoka, the Eternal") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a Dragon you control attacks, bolster 2. (Choose a creature with the least toughness " +
        "among creatures you control and put two +1/+1 counters on it.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        effect = Patterns.Mechanic.bolster(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Eric Deschamps"
        flavorText = "\"The bane of countless shattered weapons, each a failure to slay her.\"\n—Daghatar the Adamant"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a10e86-d2cd-4193-85f5-5e4908c30e7f.jpg?1783938675"
        ruling("2014-11-24", "Each Dragon you attack with will cause Dromoka's triggered ability to trigger. For each instance of the ability, you'll determine which creature to put +1/+1 counters on as it resolves. Specifically, the creature you control with the lowest toughness as the first such ability resolves may not have the lowest toughness as the second such ability resolves.")
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
