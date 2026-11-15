package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sight of the Scalelords
 * {4}{G}
 * Enchantment
 * At the beginning of combat on your turn, creatures you control with toughness 4 or greater get
 * +2/+2 and gain vigilance until end of turn.
 *
 * The group is locked in as the trigger resolves (ForEachInGroup), so a creature whose toughness
 * reaches 4 later in the turn isn't pumped, and the +2/+2 stays even if its toughness drops.
 */
val SightOfTheScalelords = card("Sight of the Scalelords") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, creatures you control with toughness 4 or greater get +2/+2 and gain vigilance until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.toughnessAtLeast(4).youControl()),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Marc Simonetti"
        flavorText = "\"The Silumgar creep around our borders and infiltrate our aeries. We must remain ever watchful.\"\n" +
            "—Golran, Dromoka captain"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5921758-4e21-4d34-8cf7-b0205d4b0912.jpg"
    }
}
