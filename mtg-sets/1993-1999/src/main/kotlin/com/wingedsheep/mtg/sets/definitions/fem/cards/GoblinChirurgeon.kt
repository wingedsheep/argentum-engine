package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Goblin Chirurgeon
 * {R}
 * Creature — Goblin Shaman
 * 0/2
 * Sacrifice a Goblin: Regenerate target creature.
 *
 * The sacrifice filter is *permanent* with the Goblin subtype, not creature — the same reading
 * [GoblinGrenade] takes, so a kindred permanent counts.
 */
val GoblinChirurgeon = card("Goblin Chirurgeon") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Shaman"
    oracleText = "Sacrifice a Goblin: Regenerate target creature."
    power = 0
    toughness = 2

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN))
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = RegenerateEffect(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54a"
        artist = "Daniel Gelon"
        flavorText = "The Chirurgeons patched up their fallen comrades with a gruesome mix of twisted limbs and mangled flesh."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b710c21-e9f5-4660-80f6-2104ec65f63f.jpg?1783947895"
    }
}
