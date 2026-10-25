package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Death-Priest of Myrkul
 * {2}{B}{B}
 * Creature — Tiefling Cleric
 * 2/2
 *
 * Skeletons, Vampires, and Zombies you control get +1/+1.
 * At the beginning of your end step, if a creature died this turn, you may pay {1}. If you do,
 * create a 1/1 black Skeleton creature token.
 *
 * The lord is one static over an any-of subtype filter, so a creature that is several of the
 * listed types still gets only +1/+1 (2021-07-23 ruling). "A creature died this turn" is an
 * intervening-if (CR 603.4) on the global condition — any player's creature counts, and the
 * ability doesn't trigger at all if nothing has died as the end step begins.
 */
val DeathPriestOfMyrkul = card("Death-Priest of Myrkul") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Tiefling Cleric"
    power = 2
    toughness = 2
    oracleText = "Skeletons, Vampires, and Zombies you control get +1/+1.\n" +
        "At the beginning of your end step, if a creature died this turn, you may pay {1}. " +
        "If you do, create a 1/1 black Skeleton creature token."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withAnySubtype("Skeleton", "Vampire", "Zombie")
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Skeleton"),
                imageUri = "https://cards.scryfall.io/normal/front/f/a/fa6fdb57-82f3-4695-b1fa-1f301ea4ef83.jpg?1783926346"
            )
        )
        description = "At the beginning of your end step, if a creature died this turn, you may pay {1}. " +
            "If you do, create a 1/1 black Skeleton creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Lius Lasahido"
        flavorText = "\"The Reaper bids you rise!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/126b2636-030a-4a0e-9307-54c3372afb52.jpg?1783926498"

        ruling(
            "2021-07-23",
            "A creature you control that's more than one of the creature types listed in the first " +
                "ability will get only +1/+1."
        )
        ruling(
            "2021-07-23",
            "Death-Priest of Myrkul doesn't need to have been on the battlefield when the creature died."
        )
        ruling(
            "2021-07-23",
            "Death-Priest of Myrkul's last ability will trigger only once during your end step, no matter " +
                "how many creatures died this turn. However, if no creatures have died so far this turn as " +
                "the end step begins, the ability won't trigger at all."
        )
    }
}
