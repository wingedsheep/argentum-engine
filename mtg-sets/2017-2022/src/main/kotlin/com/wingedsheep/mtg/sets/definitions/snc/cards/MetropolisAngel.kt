package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Metropolis Angel
 * {2}{W}{U}
 * Creature — Angel Soldier
 * 3/1
 *
 * Flying
 * Whenever you attack with one or more creatures with counters on them, draw a card.
 *
 * `Triggers.you.attacks(with)` fires once per attack declaration, filtered to declared attackers
 * that have at least one counter of any kind — exactly one card no matter how many qualify.
 */
val MetropolisAngel = card("Metropolis Angel") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Angel Soldier"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever you attack with one or more creatures with counters on them, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Creature.withAnyCounter())
        effect = Effects.DrawCards(1, EffectTarget.Controller)
        description = "Whenever you attack with one or more creatures with counters on them, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Lie Setiawan"
        flavorText = "\"Angels are living proof that power does not always lead to corruption.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/633c7e38-fdad-461c-9ecb-3e89d5b80f24.jpg?1783923079"
        ruling(
            "2022-04-29",
            "Metropolis Angel's last ability triggers whenever you attack with at least one creature " +
                "that has at least one counter on it. You draw exactly one card as it resolves, no " +
                "matter how many attacking creatures had counters on them.",
        )
    }
}
