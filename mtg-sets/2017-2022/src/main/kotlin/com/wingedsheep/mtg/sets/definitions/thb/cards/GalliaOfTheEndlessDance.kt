package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gallia of the Endless Dance
 * {R}{G}
 * Legendary Creature — Satyr
 * 2/2
 * Haste
 * Other Satyrs you control get +1/+1 and have haste.
 * Whenever you attack with three or more creatures, you may discard a card at random. If you do,
 * draw two cards.
 *
 * Goblin Chieftain's lord pair over Satyrs, and Overwhelming Instinct's three-attacker trigger.
 * The payoff is Immersturm Raider's `May(IfYouDo(discard, draw))` with the random discard — the
 * draw is gated on a card actually being discarded, so an empty hand draws nothing (ruling).
 */
val GalliaOfTheEndlessDance = card("Gallia of the Endless Dance") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Satyr"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Other Satyrs you control get +1/+1 and have haste.\n" +
        "Whenever you attack with three or more creatures, you may discard a card at random. " +
        "If you do, draw two cards."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.SATYR).youControl(),
                excludeSelf = true,
            ),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.SATYR).youControl(),
                excludeSelf = true,
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 3)
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardRandom(1), Effects.DrawCards(2))
        )
        description = "Whenever you attack with three or more creatures, you may discard a card at " +
            "random. If you do, draw two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Johannes Voss"
        flavorText = "\"There's no success like excess!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0744f08e-a588-4efe-ad56-5e9ed91dda40.jpg?1783931521"
        ruling(
            "2020-01-24",
            "Once three or more creatures you control have attacked, you'll be given the option to discard " +
                "a card at random, even if some or all of those attackers leave the battlefield before " +
                "Gallia's triggered ability resolves."
        )
        ruling(
            "2020-01-24",
            "While resolving Gallia's last ability, you can't discard multiple cards to draw more than two cards."
        )
        ruling(
            "2020-01-24",
            "You can't choose to discard a card at random if you have no cards in hand. If you have one " +
                "card in hand, you may discard it at random, even though that's not very random."
        )
    }
}
