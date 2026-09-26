package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bushi Tenderfoot // Kenzo the Hardhearted (Champions of Kamigawa #2) — a flip card (CR 710).
 *
 * Bushi Tenderfoot {W} — Creature — Human Soldier 1/1
 * "When a creature dealt damage by this creature this turn dies, flip this creature."
 *
 * Kenzo the Hardhearted — Legendary Creature — Human Samurai 3/4
 * "Double strike; bushido 2"
 *
 * Bushido is display-only vocabulary, so it is lowered to its two trigger halves (blocks /
 * becomes blocked), following `DevotedRetainer`.
 */
private val BushiTenderfootUpright = card("Bushi Tenderfoot") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "When a creature dealt damage by this creature this turn dies, flip this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.damagedCreatureDies()
        effect = Effects.Flip()
        description = "When a creature dealt damage by this creature this turn dies, flip this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/864ad989-19a6-4930-8efc-bbc077a18c32.jpg?1783944342"
    }
}

private val KenzoTheHardhearted = card("Kenzo the Hardhearted") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai"
    oracleText = "Double strike; bushido 2 (Whenever this creature blocks or becomes blocked, it gets " +
        "+2/+2 until end of turn.)"
    power = 3
    toughness = 4

    keywords(Keyword.DOUBLE_STRIKE)
    keywordAbility(KeywordAbility.bushido(2))

    // Bushido 2, half one: "Whenever this creature blocks …"
    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    // Bushido 2, half two: "… or becomes blocked, it gets +2/+2 until end of turn."
    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Bushido 2"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/864ad989-19a6-4930-8efc-bbc077a18c32.jpg?1783944342"
    }
}

val BushiTenderfoot: CardDefinition = CardDefinition.flipCard(
    unflipped = BushiTenderfootUpright,
    flipped = KenzoTheHardhearted,
)
