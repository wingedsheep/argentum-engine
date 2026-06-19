package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Clammy Prowler
 * {3}{U}
 * Enchantment Creature — Horror
 * 2/5
 * Whenever this creature attacks, another target attacking creature can't be blocked this turn.
 *
 * "Another target attacking creature" = [TargetOther] wrapping an attacking-creature target,
 * which auto-excludes the source (Clammy Prowler itself). "Can't be blocked this turn" routes
 * through the projected CANT_BE_BLOCKED ability flag — the same channel the static
 * [com.wingedsheep.sdk.scripting.CantBeBlocked] ability uses — via a floating end-of-turn grant.
 */
val ClammyProwler = card("Clammy Prowler") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Horror"
    oracleText = "Whenever this creature attacks, another target attacking creature can't be blocked this turn."
    power = 2
    toughness = 5

    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target(
            "another target attacking creature",
            TargetOther(baseRequirement = TargetCreature(filter = TargetFilter.AttackingCreature))
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "John Tedrick"
        flavorText = "Sometimes, survivors in the Floodpits hear their own drowning voices crying out for help. Only the ones who flee immediately live to speak of it."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/237f0b93-f12e-4c5f-a3d7-83e8f20f8493.jpg?1726286023"
    }
}
