package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Szarel, Genesis Shepherd — Edge of Eternities Commander #4 (canonical printing)
 * {2}{B}{R}{G} · Legendary Creature — Insect Druid · 2/5
 *
 * Flying
 * You may play lands from your graveyard.
 * Whenever you sacrifice another nontoken permanent during your turn, put a number of +1/+1
 * counters equal to Szarel's power on up to one other target creature.
 *
 * Crucible-style [MayPlayLandsFromGraveyard]; the sacrifice trigger is per permanent (CR 603.2c)
 * with "during your turn" as a [triggerRestriction] on the event, not an intervening "if". The
 * amount reads Szarel's power on resolution, falling back to last-known information when Szarel
 * has left the battlefield (the ruling).
 */
val SzarelGenesisShepherd = card("Szarel, Genesis Shepherd") {
    manaCost = "{2}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Insect Druid"
    power = 2
    toughness = 5
    oracleText = "Flying\n" +
        "You may play lands from your graveyard.\n" +
        "Whenever you sacrifice another nontoken permanent during your turn, put a number of +1/+1 counters " +
        "equal to Szarel's power on up to one other target creature."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Permanent.nontoken())
        triggerRestriction = Conditions.IsYourTurn
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), creature)
        description = "Whenever you sacrifice another nontoken permanent during your turn, put a number of " +
            "+1/+1 counters equal to Szarel's power on up to one other target creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89b61123-b10f-4bcf-b5f9-1f302f9b22d1.jpg?1783906067"

        ruling(
            "2025-07-25",
            "Szarel's second ability doesn't change the times when you can play those land cards. You can still " +
                "play lands only during your main phase when you have priority and the stack is empty."
        )
        ruling(
            "2025-07-25",
            "Szarel's second ability doesn't allow you to activate abilities (such as cycling) of land cards in your graveyard."
        )
        ruling(
            "2025-07-25",
            "If Szarel leaves the battlefield at the same time as you sacrifice one or more other nontoken permanents " +
                "during your turn (probably because you also sacrificed Szarel), its last ability triggers for each " +
                "of those other nontoken permanents."
        )
        ruling(
            "2025-07-25",
            "If Szarel leaves the battlefield before its last ability resolves, use its power as it last existed " +
                "on the battlefield to determine how many +1/+1 counters to put on the target creature."
        )
    }
}
