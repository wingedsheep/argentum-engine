package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kefnet's Last Word
 * {2}{U}{U}
 * Sorcery
 * Gain control of target artifact, creature, or enchantment. Lands you control don't untap during
 * your next untap step.
 *
 * The control change is permanent (2017-07-14 ruling). With its only target illegal on resolution
 * the whole spell fizzles, so the untap clause never applies either.
 */
val KefnetsLastWord = card("Kefnet's Last Word") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact, creature, or enchantment. Lands you control don't untap during your next untap step."

    spell {
        val t = target(TargetFilter.ArtifactCreatureOrEnchantment)
        effect = Effects.GainControl(t) then
            Effects.SkipUntap(EffectTarget.Controller, GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Clint Cearley"
        flavorText = "In his final moments, Kefnet reached into the minds of those around him, desperate for logic within the consuming chaos."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d7e5103-ee7d-40d7-bcd4-c5d1a48e9821.jpg?1783936051"
        ruling("2017-07-14", "The control-change effect of Kefnet's Last Word lasts indefinitely. It doesn't wear off during the cleanup step.")
        ruling("2017-07-14", "If the target permanent is an illegal target by the time Kefnet's Last Word resolves, the entire spell doesn't resolve. Your lands will untap during your next untap step as normal.")
        ruling("2017-07-14", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it. They'll remain attached, but an Aura's effect that affects \"you\" still affects its controller rather than you, the controller of an Equipment can move it during their next main phase, and so on.")
        ruling("2017-07-14", "No lands that you control will untap during your next untap step, even lands that aren't tapped as this spell resolves. This includes lands that enter the battlefield after this spell resolves.")
        ruling("2017-07-14", "If more than one spell says that lands you control don't untap during your next untap step, the effects will all wear off during that untap step. You'll untap lands you control during your untap step after that one.")
    }
}
