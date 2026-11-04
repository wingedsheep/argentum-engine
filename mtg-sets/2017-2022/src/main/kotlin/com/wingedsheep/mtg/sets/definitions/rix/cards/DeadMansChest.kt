package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dead Man's Chest
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature an opponent controls
 * When enchanted creature dies, exile cards equal to its power from the top of its owner's
 * library. You may cast spells from among those cards for as long as they remain exiled, and
 * mana of any type can be spent to cast them.
 *
 * "Its power" is the enchanted creature's last-known power (`DynamicAmounts.triggeringPower()`),
 * and "its owner's library" is [Player.OwnerOfTriggeringEntity] — the dead creature's owner, who
 * is not necessarily its last controller. The exiled cards get a permanent, cast-only
 * (`nonLandOnly`) play permission with `withAnyManaType`.
 */
val DeadMansChest = card("Dead Man's Chest") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature an opponent controls\n" +
        "When enchanted creature dies, exile cards equal to its power from the top of its owner's " +
        "library. You may cast spells from among those cards for as long as they remain exiled, " +
        "and mana of any type can be spent to cast them."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOpponentControls)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Pipeline {
            val chestCards = gather(
                CardSource.TopOfLibrary(DynamicAmounts.triggeringPower(), Player.OwnerOfTriggeringEntity)
            )
            exile(chestCards, Player.OwnerOfTriggeringEntity)
            run(Effects.GrantMayPlayFromExile(
                from = chestCards,
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
                nonLandOnly = true,
            ))
        }
        description = "When enchanted creature dies, exile cards equal to its power from the top " +
            "of its owner's library. You may cast spells from among those cards for as long as " +
            "they remain exiled, and mana of any type can be spent to cast them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a76ac1d9-b0df-4dec-b133-057cc5902071.jpg?1783935313"

        ruling("2018-01-19", "If Dead Man's Chest and the enchanted creature are both put into " +
            "graveyards at the same time, the last ability of Dead Man's Chest still triggers.")
        ruling("2018-01-19", "To determine how many cards to exile, use the enchanted creature's " +
            "power as it last existed on the battlefield.")
        ruling("2018-01-19", "If Dead Man's Chest enchants a creature that you own but an " +
            "opponent controls, you'll exile cards from your library.")
        ruling("2018-01-19", "The effect of Dead Man's Chest doesn't change when you can cast the " +
            "exiled cards. For example, if you exile a creature card without flash, you can cast " +
            "it only during your main phase when the stack is empty.")
        ruling("2018-01-19", "Casting an exiled card causes it to leave exile. You can't cast it " +
            "multiple times.")
    }
}
