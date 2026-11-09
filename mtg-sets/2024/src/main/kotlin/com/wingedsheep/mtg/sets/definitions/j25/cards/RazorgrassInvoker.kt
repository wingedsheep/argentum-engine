package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Razorgrass Invoker — Foundations Jumpstart #22 (canonical printing; its only printing)
 * {3}{G} · Creature — Elf Scout · 4/3
 *
 * Vigilance
 * {8}: This creature and up to one other target creature each get +3/+3 until end of turn.
 *
 * One optional target slot excluding the source ([TargetFilter.OtherCreature]) plus an untargeted
 * pump of the Invoker itself. With no target chosen the ability still resolves and pumps the
 * Invoker; with a target chosen that is illegal on resolution, the ability fizzles and the Invoker
 * gets nothing (the ruling) — the ability resolver's all-targets-illegal check gives that.
 */
val RazorgrassInvoker = card("Razorgrass Invoker") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "{8}: This creature and up to one other target creature each get +3/+3 until end of turn."
    power = 4
    toughness = 3

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{8}")
        val other = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self) then Effects.ModifyStats(3, 3, other)
        description = "This creature and up to one other target creature each get +3/+3 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Jason Rainville"
        flavorText = "\"Let the beauty of our land lull them so that they may stumble upon its hidden thorns.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41a76981-b792-49da-8c1c-34991adf3ff4.jpg?1783908863"
        ruling("2024-11-08", "You don't have to choose a target for Razorgrass Invoker's last ability. However, if you do and the target creature is an illegal target as the ability tries to resolve, it won't resolve and none of its effects will happen. Razorgrass Invoker won't get +3/+3. (Razorgrass cuts both ways, apparently.)")
    }
}
