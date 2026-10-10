package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Saruman, the White Hand — Tales of Middle-earth Commander #8
 * {1}{U}{B}{R} · Legendary Creature — Avatar Wizard · Mythic
 * 2/5
 *
 * Whenever you cast a noncreature spell, amass Orcs X, where X is that spell's mana value.
 * Goblins and Orcs you control have ward {2}.
 *
 * Modeling:
 * - The cast trigger reads the triggering spell's mana value off the trigger context
 *   ([DynamicAmounts.triggeringSpellManaValue]) and feeds it to [Effects.Amass] with "Orc".
 * - "Goblins and Orcs you control" is every permanent you control with either subtype (not only
 *   creatures — a Kindred Goblin counts). The grant reads projected subtypes, so an Army that
 *   became an Orc by amassing gains ward {2}. One grant covers a Goblin Orc, so it never has two
 *   instances (ruling 2023-06-16).
 */
val SarumanTheWhiteHand = card("Saruman, the White Hand") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 2
    toughness = 5
    oracleText = "Whenever you cast a noncreature spell, amass Orcs X, where X is that spell's mana value. " +
        "(Put X +1/+1 counters on an Army you control. It's also an Orc. If you don't control an Army, " +
        "create a 0/0 black Orc Army creature token first.)\n" +
        "Goblins and Orcs you control have ward {2}."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.Amass(DynamicAmounts.triggeringSpellManaValue(), "Orc")
        description = "Whenever you cast a noncreature spell, amass Orcs X, where X is that spell's mana value."
    }

    staticAbility {
        ability = GrantWard(
            cost = WardCost.Mana("{2}"),
            filter = GroupFilter(GameObjectFilter.Permanent.youControl().withAnySubtype("Goblin", "Orc"))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Leonardo Borazio"
        flavorText = "Over all his works a dark smoke hung and wrapped itself about the sides of Orthanc."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6487ccbf-f6cd-45a6-8514-2c458b655ef2.jpg?1783916037"
        ruling(
            "2023-06-16",
            "A creature that is both a Goblin and an Orc still only gets one instance of ward {2} from " +
                "Saruman, the White Hand's last ability."
        )
    }
}
