package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Guardian Sunmare — Aetherdrift #15
 * {3}{W}{W} · Creature — Horse Mount · 5/5
 *
 * Ward {2}
 * Whenever this creature attacks while saddled, search your library for a nonland permanent card
 * with mana value 3 or less, put it onto the battlefield, then shuffle.
 * Saddle 4
 *
 * "Attacks while saddled" is [Triggers.Attacks] plus [Conditions.SourceIsSaddled] as the
 * `triggerCondition` — the saddled state is read when the trigger would fire, i.e. as attackers are
 * declared, which is exactly the ruling ("will trigger only if that creature was saddled when it was
 * declared as an attacker"). Saddled lasts until end of turn and nothing removes it mid-turn, so the
 * CR 603.4 re-check on resolution can't diverge from the fire-time read.
 *
 * The tutor is the standard search pattern with a [SearchDestination.BATTLEFIELD] landing zone; a
 * player may always fail to find, which is what the pattern's "choose up to one" selection models.
 */
val GuardianSunmare = card("Guardian Sunmare") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Horse Mount"
    oracleText = "Ward {2}\n" +
        "Whenever this creature attacks while saddled, search your library for a nonland permanent " +
        "card with mana value 3 or less, put it onto the battlefield, then shuffle.\n" +
        "Saddle 4 (Tap any number of other creatures you control with total power 4 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"
    power = 5
    toughness = 5

    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.NonlandPermanent.manaValueAtMost(3),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    keywordAbility(KeywordAbility.saddle(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c274595-94e2-4587-9cef-b38639d6429a.jpg?1783907919"
        ruling(
            "2025-02-07",
            "“Saddled” isn't an ability that a creature has. It's just something true about " +
                "that creature. It won't stop being saddled until the turn ends or it leaves the battlefield."
        )
        ruling(
            "2025-02-07",
            "An ability that triggers when a creature “attacks while saddled” will trigger " +
                "only if that creature was saddled when it was declared as an attacker."
        )
        ruling("2025-02-07", "Creatures with saddle can attack or block as normal even if they aren't saddled.")
    }
}
