package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mayhem Devil — War of the Spark #204
 * {1}{B}{R} · Creature — Devil · 3/3
 *
 * Whenever a player sacrifices a permanent, this creature deals 1 damage to any target.
 *
 * Per-permanent sacrifice trigger watching every player (`Triggers.anyPlayer`); ANY binding, so
 * Mayhem Devil sees its own sacrifice too (ruling). Sacrifices for costs (Treasure, Food) emit the
 * same event, so the trigger resolves before the spell or ability they paid for.
 */
val MayhemDevil = card("Mayhem Devil") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Devil"
    power = 3
    toughness = 3
    oracleText = "Whenever a player sacrifices a permanent, this creature deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.anyPlayer.sacrifices(GameObjectFilter.Permanent)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
        description = "Whenever a player sacrifices a permanent, this creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Dmitry Burmak"
        flavorText = "Despite their stake in the city's survival, Rakdos cultists are loath to pass up any " +
            "opportunity to sow chaos."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17416926-168b-49b3-9231-acbb8f8a1d13.jpg?1783933394"
        ruling("2019-05-03", "A legendary permanent that is put into a graveyard because of the \"legend rule\" isn't sacrificed.")
        ruling("2019-05-03", "You control Mayhem Devil's triggered ability and choose the target, no matter who sacrificed the permanent.")
        ruling("2019-05-03", "If a permanent is sacrificed to pay a cost of a spell or ability, Mayhem Devil's ability will resolve before that spell or ability. Conversely, if a permanent is sacrificed during the resolution of a spell or ability, that spell or ability will finish resolving before Mayhem Devil's ability is put onto the stack.")
        ruling("2019-05-03", "If you sacrifice Mayhem Devil, its ability triggers.")
    }
}
