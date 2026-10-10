package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Corsairs of Umbar
 * {3}{U}
 * Creature — Human Pirate
 * 3/3
 * {2}{U}: Target Goblin, Orc, or Pirate can't be blocked this turn.
 * Whenever this creature deals combat damage to a player, amass Orcs 3.
 *
 * "Target Goblin, Orc, or Pirate" names subtypes only, so the target is any permanent with one of
 * them (`Permanent.withAnySubtype`), not just a creature. Granting the evasion after the creature is
 * already blocked doesn't unblock it (the engine only consults it at declare-blockers).
 */
val CorsairsOfUmbar = card("Corsairs of Umbar") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 3
    toughness = 3
    oracleText = "{2}{U}: Target Goblin, Orc, or Pirate can't be blocked this turn.\n" +
        "Whenever this creature deals combat damage to a player, amass Orcs 3. (Put three +1/+1 counters " +
        "on an Army you control. It's also an Orc. If you don't control an Army, create a 0/0 black Orc " +
        "Army creature token first.)"

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        val t = target(TargetFilter(GameObjectFilter.Permanent.withAnySubtype("Goblin", "Orc", "Pirate")))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Amass(3, "Orc")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Narendra Bintara Adi"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/159727a9-0bc4-497d-8fb2-4a66923726d8.jpg?1783916032"
        ruling(
            "2023-06-16",
            "Activating Corsairs of Umbar's first ability after a Goblin, Orc, or Pirate has become blocked " +
                "won't cause that creature to become unblocked."
        )
    }
}
