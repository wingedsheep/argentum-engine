package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Private Eye — Murders at Karlov Manor #223
 * {1}{W}{U} · Creature — Homunculus Detective · 3/3
 *
 * Other Detectives you control get +1/+1.
 * Whenever you draw your second card each turn, target Detective can't be blocked this turn.
 *
 * The lord half is the standard `excludeSelf = true` [GroupFilter] — Private Eye is itself a
 * Detective, and "other" is what keeps it from pumping itself.
 *
 * The second half is [Triggers.NthCardDrawn] with `n = 2`, which is the CR 121.2 "your second card
 * each turn" shape rather than "the second time you draw this turn": a single draw-two crosses the
 * threshold and fires the trigger exactly once, and cards put into hand without the word "draw"
 * (CR 121.5) never advance the count. Because MKM's Detective deck is built on investigate — Clues
 * sacrificed for extra draws — this reliably fires on the turn you crack one.
 *
 * "Target Detective" is unrestricted by controller: it may be an opponent's Detective, which is
 * rarely what you want but is what the card says. Evasion is granted with the
 * [AbilityFlag.CANT_BE_BLOCKED] grant rather than a keyword, since "can't be blocked" is not a
 * keyword ability.
 */
val PrivateEye = card("Private Eye") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Homunculus Detective"
    oracleText = "Other Detectives you control get +1/+1.\n" +
        "Whenever you draw your second card each turn, target Detective can't be blocked this turn."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.DETECTIVE).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        val detective = target(
            "detective",
            TargetCreature(filter = TargetFilter.Creature.withSubtype(Subtype.DETECTIVE))
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, detective)
        description = "Whenever you draw your second card each turn, target Detective can't be " +
            "blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Vincent Christiaens"
        flavorText = "\"Yep. It's a vase, all right.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6a50807-058e-45dc-847c-8ffd13b1bd48.jpg?1783912840"
    }
}
