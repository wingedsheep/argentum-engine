package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sarkhan's Whelp
 * {2}{R}
 * Creature — Dragon
 * 2/2
 * Flying
 * Whenever you activate an ability of a Sarkhan planeswalker, this creature deals 1 damage to any target.
 *
 * "An ability" is unqualified, so it isn't limited to loyalty abilities and mana abilities count
 * too (the Ceaseless Searblades reading). "A Sarkhan planeswalker" is a permanent on the
 * battlefield; whose it is doesn't matter — only that you activate it.
 */
val SarkhansWhelp = card("Sarkhan's Whelp") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever you activate an ability of a Sarkhan planeswalker, this creature deals 1 damage to any target."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(
            of = GameObjectFilter.Planeswalker.withSubtype("Sarkhan").onBattlefield(),
            includeManaAbilities = true,
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
        description = "Whenever you activate an ability of a Sarkhan planeswalker, this creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "299"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07135c4d-b4de-4054-800a-11090ed32692.jpg?1783934487"
    }
}
