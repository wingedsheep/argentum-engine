package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Ominous Roost
 * {2}{U}
 * Enchantment
 * When this enchantment enters and whenever you cast a spell from your graveyard, create a 1/1
 * blue Bird creature token with flying and "This token can block only creatures with flying."
 *
 * The combined "when … and whenever …" text is two triggered abilities sharing one effect: an
 * enters trigger and a cast trigger gated by [SpellCastPredicate.CastFromZone] on the graveyard
 * (flashback, disturb, etc. — the Burning Vengeance shape).
 */
private val createRoostBird = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.BLUE),
    creatureTypes = setOf("Bird"),
    keywords = setOf(Keyword.FLYING),
    staticAbilities = listOf(
        CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    ),
    imageUri = "https://cards.scryfall.io/normal/front/a/0/a0f7b2f0-16d3-4db0-a737-c7b8dcb9d5de.jpg?1783925225",
)

val OminousRoost = card("Ominous Roost") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters and whenever you cast a spell from your graveyard, " +
        "create a 1/1 blue Bird creature token with flying and \"This token can block only creatures with flying.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = createRoostBird
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.GRAVEYARD)))
        effect = createRoostBird
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Josu Hernaiz"
        flavorText = "\"One raven is a bad omen. Two are a curse. I don't know what this is, but it can't be good.\"\n—Verna, priest of Sigarda"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daf2b49a-56f9-4093-960f-9ec7af8983b5.jpg?1783925636"
    }
}
