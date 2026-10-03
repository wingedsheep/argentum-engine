package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gigantoplasm — {3}{U} Creature — Shapeshifter 0/0.
 *
 * The "{X}: … X/X" ability rides [CopyExceptions.addedActivatedAbilities], so it is a copiable value
 * (a Clone of Gigantoplasm has it too). Its effect has no duration: a Layer 7b set that lasts until
 * the permanent leaves or a later set overwrites it.
 */
val Gigantoplasm = card("Gigantoplasm") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except it has \"{X}: This creature has base power and toughness X/X.\""

    replacementEffect(EntersAsCopy(
        exceptions = CopyExceptions(addedActivatedAbilities = listOf(
            grantedActivatedAbility {
                cost = Costs.Mana("{X}")
                effect = Effects.SetBasePowerAndToughness(
                    power = DynamicAmounts.xValue(),
                    toughness = DynamicAmounts.xValue(),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent,
                )
            }
        ))
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Kev Walker"
        flavorText = "\"I appreciate things that exceed expectations.\"\n—Mizzix"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27163b43-61e2-495c-b544-55c349eba99c.jpg?1783938115"
        ruling("2015-11-04", "The activated ability Gigantoplasm gives itself becomes part of its copiable values. Unless the ability is overwritten by another copy effect, a creature that's a copy of Gigantoplasm will have that ability.")
        ruling("2015-11-04", "However, the effect of that ability isn't copiable. That is, if Gigantoplasm is a copy of a creature with base power and toughness 2/2 and you activate its ability making it a 4/4 creature, another creature that becomes a copy of Gigantoplasm will have base power and toughness 2/2.")
        ruling("2015-11-04", "Effects that modify Gigantoplasm's power and/or toughness, such as the effect of Giant Growth or Glorious Anthem, will apply to Gigantoplasm no matter when they started applying. The same is true for counters that affect its power and/or toughness and effects that switch its power and toughness.")
        ruling("2015-11-04", "You can choose not to copy anything. In that case, Gigantoplasm enters the battlefield as a 0/0 Shapeshifter creature, and is probably put into the graveyard immediately.")
    }
}
