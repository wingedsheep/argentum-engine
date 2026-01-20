package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Bile-Vial Boggart
 *
 * {B} Creature — Goblin Assassin 1/1
 * When this creature dies, put a -1/-1 counter on up to one target creature.
 */
object BileVialBoggart {
    val definition = CardDefinition.creature(
        name = "Bile-Vial Boggart",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.ASSASSIN),
        power = 1,
        toughness = 1,
        oracleText = "When this creature dies, put a -1/-1 counter on up to one target creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "87",
            rarity = Rarity.COMMON,
            artist = "Slawomir Maniak",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee1e1e1e-1e1e-1e1e-1e1e-1e1e1e1e1e1e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Bile-Vial Boggart") {
        // Death trigger: Put a -1/-1 counter on target creature
        triggered(
            trigger = OnDeath(),
            effect = AddCountersEffect(
                counterType = "-1/-1",
                count = 1,
                target = EffectTarget.TargetCreature
            ),
            optional = true  // "up to one"
        )
    }
}
