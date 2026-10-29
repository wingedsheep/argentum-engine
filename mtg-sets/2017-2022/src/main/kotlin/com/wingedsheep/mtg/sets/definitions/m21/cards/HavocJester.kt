package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Havoc Jester
 * {4}{R}
 * Creature — Devil
 * 5/5
 * Whenever you sacrifice a permanent, this creature deals 1 damage to any target.
 *
 * The per-permanent `Triggers.you.sacrifices(Permanent)` (Blood Aspirant shape): it fires once
 * for each permanent sacrificed, including Havoc Jester itself (rulings).
 */
val HavocJester = card("Havoc Jester") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 5
    toughness = 5
    oracleText = "Whenever you sacrifice a permanent, this creature deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Permanent)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(1, any)
        description = "Whenever you sacrifice a permanent, this creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Jason A. Engle"
        flavorText = "To the Rakdos, screams of delight and screams of terror are one and the same."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/386e5cb2-39c8-453d-a642-c5d9f8495601.jpg?1783930689"
        ruling("2020-06-23", "Havoc Jester's ability is a triggered ability, not an activated ability. It doesn't allow you to sacrifice a permanent whenever you want; rather, you need some other way of sacrificing permanents.")
        ruling("2020-06-23", "If you sacrifice a permanent as part of casting a spell or activating an ability, Havoc Jester's ability will resolve before that spell or ability.")
        ruling("2020-06-23", "Havoc Jester's ability triggers when you sacrifice it. If you sacrifice other permanents at the same time, it triggers for them as well.")
        ruling("2020-06-23", "A legendary permanent that is put into a graveyard because of the \"legend rule\" isn't sacrificed.")
    }
}
