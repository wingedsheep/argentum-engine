package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angel of Grace — Ravnica Allegiance #1
 * {3}{W}{W} · Creature — Angel · 5 / 4
 *
 * - The enters trigger grants Ali from Cairo's [LifeLossFloor] for the turn, anchored to the
 *   Angel: the grant is "you"-scoped to the player who controlled it as the trigger resolved and
 *   keeps applying after the Angel leaves the battlefield.
 * - The graveyard ability is instant speed (no timing restriction printed).
 */
val AngelOfGrace = card("Angel of Grace") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 4
    oracleText = "Flash\nFlying\nWhen this creature enters, until end of turn, damage that would reduce " +
        "your life total to less than 1 reduces it to 1 instead.\n" +
        "{4}{W}{W}, Exile this card from your graveyard: Your life total becomes 10."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantReplacementEffect(
            replacement = LifeLossFloor(floor = 1, appliesTo = EventPattern.LifeLossEvent(player = Player.You)),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}{W}"), Costs.ExileSelf)
        effect = Effects.SetLifeTotal(10, EffectTarget.Controller)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80164e61-3e94-4e10-9bd1-518b8dc7fc4c.jpg?1783933727"
    }
}
