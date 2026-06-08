package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Magebane Lizard
 * {1}{R}
 * Creature — Lizard
 * 1/4
 * Whenever a player casts a noncreature spell, this creature deals damage to that player equal to the
 * number of noncreature spells they've cast this turn.
 */
val MagebaneLizard = card("Magebane Lizard") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 1
    toughness = 4
    oracleText = "Whenever a player casts a noncreature spell, this creature deals damage to that player " +
        "equal to the number of noncreature spells they've cast this turn."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(
            amount = DynamicAmount.SpellsCastThisTurn(Player.TriggeringPlayer, GameObjectFilter.Noncreature),
            target = EffectTarget.ControllerOfTriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Camille Alquier"
        flavorText = "\"I didn't miss. Blasted thing snatched my bolt out of the air and ate it!\"\n" +
            "—Lilah, Slickshot boss"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62e12566-375f-4f31-aa91-1b13a96d9ece.jpg?1712355797"

        ruling("2024-04-12", "Magebane Lizard's ability counts the noncreature spell that's currently being cast, as that spell is already on the stack when the ability triggers and resolves.")
        ruling("2024-04-12", "The number of noncreature spells is counted as Magebane Lizard's ability resolves, so a noncreature spell cast in response will be counted.")
    }
}
