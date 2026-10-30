package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindwrack Harpy
 * {3}{B}
 * Enchantment Creature — Harpy
 * 3/2
 * Flying
 * At the beginning of combat on your turn, each player mills three cards.
 */
val MindwrackHarpy = card("Mindwrack Harpy") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Harpy"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "At the beginning of combat on your turn, each player mills three cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
        description = "At the beginning of combat on your turn, each player mills three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "276"
        artist = "Deruchenko Alexander"
        flavorText = "Harpies are among the most detested creatures in all of Theros... and they thoroughly " +
            "deserve it."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c5ce79a-fcbd-46ab-8911-480f967ca57f.jpg?1783931498"
    }
}
