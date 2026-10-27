package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * River Kelpie
 * {3}{U}{U}
 * Creature — Beast
 * 3/3
 * Whenever this creature or another permanent enters from a graveyard, draw a card.
 * Whenever a player casts a spell from a graveyard, draw a card.
 * Persist
 *
 * "This creature or another permanent" is any permanent, River Kelpie included — so Kelpie
 * returning from your graveyard through its own persist trigger draws a card. "From a graveyard"
 * is any player's graveyard, for both the entering permanent and the cast spell.
 */
val RiverKelpie = card("River Kelpie") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature or another permanent enters from a graveyard, draw a card.\n" +
        "Whenever a player casts a spell from a graveyard, draw a card.\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the " +
        "battlefield under its owner's control with a -1/-1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent).enters(from = Zone.GRAVEYARD)
        effect = Effects.DrawCards(1)
        description = "Whenever this creature or another permanent enters from a graveyard, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.GRAVEYARD)))
        effect = Effects.DrawCards(1)
        description = "Whenever a player casts a spell from a graveyard, draw a card."
    }

    keywords(Keyword.PERSIST)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Jeff Easley"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/970adaaf-1534-4529-8da4-c4dcf7c08b7b.jpg?1783942758"
        ruling(
            "2008-05-01",
            "If River Kelpie and another permanent are each put onto the battlefield from a graveyard at " +
                "the same time, River Kelpie's first ability will trigger twice. (It will see the other " +
                "permanent entering.)"
        )
        ruling(
            "2008-05-01",
            "If you cast another artifact, creature, enchantment, or planeswalker spell from a graveyard, " +
                "only the second ability triggers. That's because the card is put onto the stack, not onto " +
                "the battlefield."
        )
        ruling(
            "2008-05-01",
            "If you cast River Kelpie itself from your graveyard (by using Yawgmoth's Will, for example), " +
                "neither ability triggers. The first ability doesn't trigger because River Kelpie is put " +
                "onto the stack; the second ability doesn't trigger because it works only while River " +
                "Kelpie is already on the battlefield."
        )
        ruling(
            "2008-05-01",
            "If you play a land card from a graveyard (by using Crucible of Worlds, for example), only the " +
                "first ability triggers. That's because the land (which isn't a spell) is put directly onto " +
                "the battlefield from the graveyard."
        )
    }
}
