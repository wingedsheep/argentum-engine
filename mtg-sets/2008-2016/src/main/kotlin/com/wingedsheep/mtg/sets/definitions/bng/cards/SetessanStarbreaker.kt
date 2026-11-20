package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Setessan Starbreaker — Born of the Gods #139 (canonical printing)
 * {3}{G}
 * Creature — Human Warrior
 * 2/1
 * When this creature enters, you may destroy target Aura.
 */
val SetessanStarbreaker = card("Setessan Starbreaker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may destroy target Aura."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val aura = target(TargetFilter.Enchantment.withSubtype("Aura"))
        effect = Effects.Destroy(aura)
        description = "When this creature enters, you may destroy target Aura."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Chase Stone"
        flavorText = "\"Who says you cannot touch the stars?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03af8b3b-a897-43e2-a87d-833da93edc41.jpg?1783939528"
    }
}
