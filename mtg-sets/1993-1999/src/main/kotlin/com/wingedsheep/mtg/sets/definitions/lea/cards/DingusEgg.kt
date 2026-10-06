package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dingus Egg — Limited Edition Alpha #241
 * {4} · Artifact
 *
 * Whenever a land is put into a graveyard from the battlefield, this artifact deals 2 damage to
 * that land's controller.
 *
 * Any land's battlefield-to-graveyard move (`Triggers.a(Land).dies()`, the same event as "dies"
 * for a noncreature permanent). "That land's controller" is [Player.TriggeringPlayer], which a
 * zone-change trigger binds to the land's last-known controller as it left the battlefield — not
 * its owner, so a stolen land damages the player who controlled it (Massacre Wurm's shape).
 */
val DingusEgg = card("Dingus Egg") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a land is put into a graveyard from the battlefield, this artifact deals " +
        "2 damage to that land's controller."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land).dies()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65eb6cda-e512-40a8-9c1f-335b713409ff.jpg?1783948668"
    }
}
