package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kederekt Parasite
 * {B}
 * Creature — Horror
 * 1/1
 * Whenever an opponent draws a card, if you control a red permanent, you may have this creature
 * deal 1 damage to that player.
 *
 * Underworld Dreams' trigger with Conflux's intervening "if" (CR 603.4, Parasitic Strix's
 * [Conditions.YouControl] over a red permanent — checked on trigger and again on resolution) and
 * an optional ("you may") damage.
 */
val KederektParasite = card("Kederekt Parasite") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 1
    toughness = 1
    oracleText = "Whenever an opponent draws a card, if you control a red permanent, you may have this creature deal 1 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.withColor(Color.RED))
        effect = Effects.May(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Dan Murayama Scott"
        flavorText = "When the smell of passing thoughts piques its hunger, its maw becomes primed with acid and a taste for brains."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/878c7d8c-4df0-43ac-8197-d89c8be5e70d.jpg?1783942482"
        ruling(
            "2009-02-01",
            "The \"intervening 'if' clause\" means that (1) the ability won't trigger at all unless you control a permanent of the specified color, and (2) the ability will do nothing unless you control a permanent of the specified color at the time it resolves.",
        )
    }
}
