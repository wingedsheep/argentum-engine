package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Khan, Despot by Design — Star Trek #155
 * {4}{R} · Legendary Creature — Human Rogue · 5/4
 *
 * Trample
 * When Khan enters, he deals 1 damage to each opponent and each creature your opponents control.
 */
val KhanDespotByDesign = card("Khan, Despot by Design") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Rogue"
    power = 5
    toughness = 4
    oracleText = "Trample\nWhen Khan enters, he deals 1 damage to each opponent and each creature your opponents control."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.opponentControls()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Kirk DouPonce"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd283ef1-fd8c-4288-8efa-6e81c92b3415.jpg?1793013585"
    }
}
