package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bloodchief's Thirst — Zendikar Rising #94
 * {B} · Sorcery
 *
 * The kicked clause replaces the target restriction (CR 601.2c — targets are chosen with the
 * kicker decision already made), so it rides the `kickerTarget` / `kickerEffect` branch like
 * Urborg Repossession: unkicked targets a creature or planeswalker with mana value 2 or less,
 * kicked targets any creature or planeswalker.
 */
val BloodchiefsThirst = card("Bloodchief's Thirst") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{B} (You may pay an additional {2}{B} as you cast this spell.)\n" +
        "Destroy target creature or planeswalker with mana value 2 or less. If this spell was kicked, " +
        "instead destroy target creature or planeswalker."

    keywordAbility(KeywordAbility.kicker("{2}{B}"))

    spell {
        val small = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(2)))
        effect = Effects.Destroy(small)

        val any = kickerTarget(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        kickerEffect = Effects.Destroy(any)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Jason Rainville"
        flavorText = "\"We are free. It is our duty to live well.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/059e8447-6b1c-4651-a734-a8fea2cbf7b2.jpg?1783929380"
    }
}
