package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Fury
 * {4}{R}
 * Instant
 * Chandra's Fury deals 4 damage to target player or planeswalker and 1 damage to each creature that
 * player or that planeswalker's controller controls.
 *
 * Only the recipient is targeted, so a hexproof creature it covers is still dealt 1 damage (ruling).
 * The two amounts are separate instructions with no "other": a target planeswalker that is also a
 * creature is one of "each creature that planeswalker's controller controls", so it is dealt 4 and 1.
 */
val ChandrasFury = card("Chandra's Fury") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chandra's Fury deals 4 damage to target player or planeswalker and 1 damage to each " +
        "creature that player or that planeswalker's controller controls."

    spell {
        val recipient = target(Targets.PlayerOrPlaneswalker)
        val creatures = GameObjectFilter.Creature.targetPlayerControls(recipient) or
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
        effect = Effects.DealDamage(4, recipient) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(creatures))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Volkan Baǵa"
        flavorText = "\"I asked if they wanted to do things the easy way. I meant easy for me.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25335fee-d320-4622-bcf4-292400dee52b.jpg?1783940486"

        ruling("2012-07-01", "Chandra's Fury targets only the player, not any creature. Chandra's Fury will deal 1 damage to a creature with hexproof, for example.")
    }
}
