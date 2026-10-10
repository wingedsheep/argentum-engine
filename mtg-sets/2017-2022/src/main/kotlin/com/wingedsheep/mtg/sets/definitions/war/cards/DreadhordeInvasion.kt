package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreadhorde Invasion — War of the Spark #86 (canonical printing)
 * {1}{B}
 * Enchantment
 * At the beginning of your upkeep, you lose 1 life and amass Zombies 1.
 * Whenever a Zombie token you control with power 6 or greater attacks, it gains lifelink until
 * end of turn.
 *
 * The second ability is the per-attacker `Triggers.a(filter).attacks()` shape, so each qualifying
 * attacker triggers separately and "it" is [EffectTarget.TriggeringEntity]. The power check is a
 * trigger-time filter, not an intervening "if": per the 2019-05-03 rulings, power is checked only
 * immediately after the token attacks, and later changes don't take lifelink away. Any Zombie
 * token qualifies, not only an amassed Army (2023-06-16 ruling).
 */
val DreadhordeInvasion = card("Dreadhorde Invasion") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you lose 1 life and amass Zombies 1. (Put a +1/+1 " +
        "counter on an Army you control. It's also a Zombie. If you don't control an Army, create a 0/0 " +
        "black Zombie Army creature token first.)\n" +
        "Whenever a Zombie token you control with power 6 or greater attacks, it gains lifelink until " +
        "end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.Amass(1, "Zombie")
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withSubtype("Zombie").token().youControl().powerAtLeast(6)
        ).attacks()
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Stanton Feng"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04c6e42d-991d-4e6b-a900-38480931f79e.jpg?1783933447"
        ruling("2019-05-03", "The power of the Zombie token is checked only immediately after it attacks. Any other abilities that trigger when creatures you control attack won't have resolved yet.")
        ruling("2019-05-03", "If the token's power changes later in the turn after it has attacked, including while Dreadhorde Invasion's second ability is on the stack, it won't cause the token to gain or lose lifelink.")
        ruling("2023-06-16", "Some cards that cause you to amass Zombies also provide bonuses to \"Zombie tokens.\" These affect any token that happens to be a Zombie, not just a Zombie Army you've amassed.")
    }
}
