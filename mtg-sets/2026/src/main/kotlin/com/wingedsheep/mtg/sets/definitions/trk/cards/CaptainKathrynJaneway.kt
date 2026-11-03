package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captain Kathryn Janeway
 * {3}{G}
 * Legendary Creature — Human Officer
 * 2/3
 *
 * You may play an additional land on each of your turns.
 * Whenever Captain Janeway or another creature you control enters, that creature explores.
 *
 * Exploration's [GrantAdditionalLandDrop] plus Path of Discovery's trigger: "Janeway or another
 * creature you control" is every creature you control, so `Triggers.a(...)` (which includes the
 * source itself) and the entering creature as [EffectTarget.TriggeringEntity].
 */
val CaptainKathrynJaneway = card("Captain Kathryn Janeway") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 3
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Whenever Captain Janeway or another creature you control enters, that creature explores. " +
        "(Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, " +
        "put a +1/+1 counter on the creature, then put the card back or put it into your graveyard.)"

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.Explore(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Jason Pastrana"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d17ea45c-7f9c-411f-b7d3-ca7100ffe53b.jpg?1784036788"
    }
}
