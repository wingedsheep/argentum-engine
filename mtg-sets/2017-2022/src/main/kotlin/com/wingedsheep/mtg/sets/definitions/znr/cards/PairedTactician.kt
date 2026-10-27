package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Paired Tactician — Zendikar Rising #31
 * {2}{W} · Creature — Human Warrior · 3/2
 *
 * Whenever this creature and at least one other Warrior attack, put a +1/+1 counter on this
 * creature.
 *
 * "And at least one other Warrior attack" qualifies the attack event, so it is a
 * `triggerRestriction` over the other attacking Warriors — checked once as the trigger fires,
 * never rechecked on resolution (Valor's Reach Tag Team's token shape). `excludeSelf` keeps the
 * Tactician from counting itself, and the Warrior check reads projected subtypes, so the
 * Tactician itself needn't be a Warrior.
 */
val PairedTactician = card("Paired Tactician") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature and at least one other Warrior attack, put a +1/+1 counter " +
        "on this creature."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        triggerRestriction = Conditions.YouControl(
            GameObjectFilter.Creature.withSubtype("Warrior").attacking(),
            excludeSelf = true,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature and at least one other Warrior attack, put a +1/+1 " +
            "counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Colin Boyer"
        flavorText = "\"If we fight as one, nothing can stand in our way!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67930e10-4de8-4d40-af27-4a38d35ab16f.jpg?1783929410"

        ruling("2020-09-25", "Paired Tactician gets just one +1/+1 counter from its ability, no matter how many Warriors attack with it beyond the first.")
        ruling("2020-09-25", "The other attacking Warrior doesn't have to be attacking the same player or planeswalker as Paired Tactician.")
    }
}
