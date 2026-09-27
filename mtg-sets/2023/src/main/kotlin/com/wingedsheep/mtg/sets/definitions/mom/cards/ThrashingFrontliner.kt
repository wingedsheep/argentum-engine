package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thrashing Frontliner — March of the Machine #167
 * {1}{R} · Creature — Phyrexian Lizard · 2/2
 *
 * Trample
 * Whenever this creature attacks a battle, it gets +1/+1 until end of turn.
 *
 * "attacks a battle" is [AttackPredicate.DefenderIsBattle]: attacking a player or planeswalker
 * doesn't trigger it.
 */
val ThrashingFrontliner = card("Thrashing Frontliner") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Lizard"
    oracleText = "Trample\nWhenever this creature attacks a battle, it gets +1/+1 until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.DefenderIsBattle))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Pavel Kolomeyets"
        flavorText = "With Urabrask in hiding, the masterless Furnace Host was unleashed on the Multiverse to spread mayhem and destruction."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77fd9ccb-2cb0-4fe1-9eb0-ba929bb527be.jpg?1783916980"
    }
}
