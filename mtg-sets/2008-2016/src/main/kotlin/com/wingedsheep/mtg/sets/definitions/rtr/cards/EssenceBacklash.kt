package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Essence Backlash
 * {2}{U}{R}
 * Instant
 * Counter target creature spell. Essence Backlash deals damage equal to that spell's power to its
 * controller.
 *
 * The damage resolves while the target is still on the stack, so `TargetController` and the
 * spell's power read the live stack object (a creature spell's power on the stack is its printed or
 * CDA-defined value — ruling 2012-10-01); countering first would move it off the stack. Both happen
 * in one resolution, so the order is imperceptible. The damage isn't gated on the counter: a
 * creature spell that can't be countered still costs its controller the damage (ruling).
 */
val EssenceBacklash = card("Essence Backlash") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Counter target creature spell. Essence Backlash deals damage equal to that spell's power " +
        "to its controller."

    spell {
        val spell = target(TargetFilter.CreatureSpellOnStack)
        effect = Effects.DealDamage(DynamicAmounts.powerOf(spell), EffectTarget.TargetController) then
            Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Jung Park"
        flavorText = "\"The other guilds will learn the hard way not to interfere with our investigations.\"\n" +
            "—Bori Andon, Izzet blastseeker"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98609dc-ea90-4c7e-a191-5e5d0ba16847.jpg?1783940341"
        ruling(
            "2012-10-01",
            "Essence Backlash can target a creature spell that can't be countered. If Essence Backlash " +
                "resolves, it won't counter the spell but it will still deal damage."
        )
        ruling(
            "2012-10-01",
            "If the creature's power is * and it has an ability defining its power, use that ability to " +
                "determine its power."
        )
        ruling(
            "2012-10-01",
            "Abilities that change the power of a creature (such as the one of Collective Blessing) affect " +
                "only creatures on the battlefield. They won't change a creature spell's power on the stack."
        )
    }
}
