package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sawblade Slinger
 * {3}{G}
 * Creature — Human Archer
 * 4/3
 *
 * When this creature enters, choose up to one —
 * • Destroy target artifact an opponent controls.
 * • This creature fights target Zombie an opponent controls.
 *
 * ETB "choose up to one" modal — a [ModalEffect] with `chooseCount = 1, minChooseCount = 0`
 * so the controller may decline both modes (the HighwayRobbery min-0 idiom). Mode 1 destroys
 * an opponent's artifact; mode 2 is a self-fight against an opponent's Zombie (source is
 * [EffectTarget.Self], the HivespineWolverine idiom). Both target restrictions use
 * `opponentControls()` so only opposing permanents are legal.
 */
val SawbladeSlinger = card("Sawblade Slinger") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, choose up to one —\n" +
        "• Destroy target artifact an opponent controls.\n" +
        "• This creature fights target Zombie an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Destroy target artifact an opponent controls") {
                    val artifact = target(TargetFilter(GameObjectFilter.Artifact.opponentControls()))
                    effect = Effects.Destroy(artifact)
                },
                mode("This creature fights target Zombie an opponent controls") {
                    val creature = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Zombie").opponentControls()))
                    effect = Effects.Fight(EffectTarget.Self, creature)
                }
            ),
            chooseCount = 1,
            minChooseCount = 0,
            countsAsModalSpell = false
        )
        description = "When this creature enters, choose up to one — Destroy target artifact an " +
            "opponent controls, or this creature fights target Zombie an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/225773f9-1843-4ee0-8564-8e4a5dfef775.jpg?1782703040"
    }
}
