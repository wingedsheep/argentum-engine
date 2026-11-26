package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nefarox, Overlord of Grixis
 * {4}{B}{B}
 * Legendary Creature — Demon
 * 5/5
 * Flying
 * Exalted
 * Whenever Nefarox attacks alone, defending player sacrifices a creature of their choice.
 */
val NefaroxOverlordOfGrixis = card("Nefarox, Overlord of Grixis") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    oracleText = "Flying\n" +
        "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)\n" +
        "Whenever Nefarox attacks alone, defending player sacrifices a creature of their choice."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.EXALTED)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.Alone))
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abc382f3-fdb9-4987-acf4-bf1ac4fd2ef7.jpg?1783940497"
    }
}
