package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Honored Crop-Captain
 * {R}{W}
 * Creature — Human Warrior
 * 3/2
 * Whenever this creature attacks, other attacking creatures get +1/+0 until end of turn.
 *
 * The attacking group is read on resolution, after all attackers are declared, so every creature
 * attacking alongside the Captain gets the bonus — but not the Captain itself.
 */
val HonoredCropCaptain = card("Honored Crop-Captain") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever this creature attacks, other attacking creatures get +1/+0 until end of turn."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures.other(),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Sara Winters"
        flavorText = "Her words inspire action. Her actions inspire victory."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1b44194-fed5-491c-a791-80b986dd66d6.jpg"
    }
}
