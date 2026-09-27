package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * War-Trained Slasher — March of the Machine #172
 * {3}{R} · Creature — Wolverine Dinosaur · 4/3
 *
 * Menace
 * Whenever this creature attacks a battle, double its power until end of turn.
 *
 * "Double its power" is +X/+0 where X is its power as the ability resolves (the Grunn, the Lonely
 * King composition), so the bonus locks in and isn't re-doubled.
 */
val WarTrainedSlasher = card("War-Trained Slasher") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolverine Dinosaur"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks a battle, double its power until end of turn."
    power = 4
    toughness = 3

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.DefenderIsBattle))
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.fixed(0), EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Francis Tneh"
        flavorText = "\"That's my girl. Just don't eat it—wouldn't want you getting an upset stomach again.\"\n—Jalina, Ikorian bonder"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2a87c5d-5efc-4ceb-b165-27f807117f71.jpg?1783916979"
    }
}
