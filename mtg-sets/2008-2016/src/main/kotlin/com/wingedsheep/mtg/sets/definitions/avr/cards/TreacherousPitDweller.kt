package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Treacherous Pit-Dweller — Avacyn Restored #121.
 * {B}{B} Creature — Demon 4/3
 *
 * The enters-from-a-graveyard trigger fires on any graveyard-to-battlefield entry, its own undying
 * return included; the targeted opponent then gains control of it permanently.
 */
val TreacherousPitDweller = card("Treacherous Pit-Dweller") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "When this creature enters from a graveyard, target opponent gains control of it.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the " +
        "battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 4
    toughness = 3
    keywords(Keyword.UNDYING)

    triggeredAbility {
        trigger = Triggers.self.enters(from = Zone.GRAVEYARD)
        val opponent = target(Targets.Opponent)
        effect = Effects.GiveControl(permanent = EffectTarget.Self, newController = opponent)
        description = "When this creature enters from a graveyard, target opponent gains control of it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eec7dfd7-d7b2-44fa-b351-022a19fe81b8.jpg?1783940690"
    }
}
