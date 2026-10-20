package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pia Nalaar, Consul of Revival — March of the Machine: The Aftermath #42
 * {R}{W} · Legendary Creature — Human Artificer · 2/3
 *
 * Thopters you control have haste.
 * Whenever you play a land from exile or cast a spell from exile, create a 1/1 colorless Thopter
 * artifact creature token with flying.
 *
 * The "play a land or cast a spell" trigger is two triggered abilities, one per event: a land play
 * from exile (`playsLandFrom(Zone.EXILE)`) and a spell cast from exile.
 */
val PiaNalaarConsulOfRevival = card("Pia Nalaar, Consul of Revival") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "Thopters you control have haste.\n" +
        "Whenever you play a land from exile or cast a spell from exile, create a 1/1 colorless " +
        "Thopter artifact creature token with flying."

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.THOPTER).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.playsLandFrom(Zone.EXILE)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.EXILE)))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ae89461-4bce-4b49-b875-03afc2469fe7.jpg?1783916510"
    }
}
