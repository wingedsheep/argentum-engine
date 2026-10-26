package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * General Kreat, the Boltbringer — Foundations Jumpstart #48
 * {2}{R} · Legendary Creature — Goblin Soldier · 2/2
 *
 * Whenever one or more Goblins you control attack, create a 1/1 red Goblin creature token that's
 * tapped and attacking.
 * Whenever another creature you control enters, General Kreat deals 1 damage to each opponent.
 *
 * The attack trigger is the batch `Triggers.you.attacks(with)` shape (Prodigy's Prototype): one
 * token per attack declaration however many Goblins attack. The token's defender is picked by its
 * controller as it enters (CR 508.4, per the ruling) — [EnteringAttackDefender.CONTROLLER_CHOOSES].
 */
val GeneralKreatTheBoltbringer = card("General Kreat, the Boltbringer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever one or more Goblins you control attack, create a 1/1 red Goblin creature " +
        "token that's tapped and attacking.\n" +
        "Whenever another creature you control enters, General Kreat deals 1 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/7/0/70f8a1de-cd4c-4afa-bf03-0245d375d42e.jpg?1783908589"
        )
        description = "Whenever one or more Goblins you control attack, create a 1/1 red Goblin creature " +
            "token that's tapped and attacking."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever another creature you control enters, General Kreat deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Takeuchi Moto"
        flavorText = "To cross swords with her is a shockingly bad idea."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/226fc101-abcc-4ed4-8c0b-3677dc8d8f0a.jpg?1783908856"
        ruling("2024-11-08", "Although the token created by General Kreat's first ability enters attacking, it was never declared as an attacker. Abilities that trigger whenever a creature attacks won't trigger.")
        ruling("2024-11-08", "You choose which player, planeswalker, or battle the token is attacking. It doesn't need to be the same player, planeswalker, or battle that any of the other Goblins are attacking.")
        ruling("2024-11-08", "If General Kreat enters at the same time as one or more other creatures you control, its last ability will trigger for each of those creatures.")
    }
}
