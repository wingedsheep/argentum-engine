package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Squee, Goblin Nabob
 * {2}{R}
 * Legendary Creature — Goblin
 * 1 / 1
 *
 * The ability functions from the graveyard, not the battlefield (CR 113.6b), so the trigger is
 * zone-scoped with `triggerZone = Zone.GRAVEYARD` — the same arrangement as Gigapede. Without it
 * the upkeep trigger would only be indexed while Squee is on the battlefield, where the ability
 * can never do anything.
 */
val SqueeGoblinNabob = card("Squee, Goblin Nabob") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin"
    oracleText = "At the beginning of your upkeep, you may return this card from your graveyard to your hand."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerZone = Zone.GRAVEYARD
        optional = true
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "David Monette"
        flavorText = "\"General?!\" Tahngarth roared. \"General *nuisance*, maybe.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4ba8325a-1203-4125-9111-94d9e2b1f14b.jpg"
    }
}
