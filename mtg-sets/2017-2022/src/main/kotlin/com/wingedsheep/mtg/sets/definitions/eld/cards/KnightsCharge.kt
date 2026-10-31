package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knights' Charge — Throne of Eldraine #328 (Brawl deck)
 * {1}{W}{B} · Enchantment
 *
 * Whenever a Knight you control attacks, each opponent loses 1 life and you gain 1 life.
 * {6}{W}{B}, Sacrifice this enchantment: Return all Knight creature cards from your graveyard to
 * the battlefield.
 *
 * The trigger is per-attacker (Militia's Pride's shape), so attacking with three Knights drains
 * three times. The gain is a flat 1, not "life equal to the life lost" — Dranas Emissary's
 * `LoseLife(EachOpponent) then GainLife(1)`, not a DrainLife.
 *
 * The activated ability is Zombie Apocalypse's gather-then-move over the controller's graveyard,
 * untapped. Sacrifice is part of the cost, so this enchantment is already gone when it resolves.
 */
val KnightsCharge = card("Knights' Charge") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Whenever a Knight you control attacks, each opponent loses 1 life and you gain 1 life.\n" +
        "{6}{W}{B}, Sacrifice this enchantment: Return all Knight creature cards from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Knight").youControl()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}{W}{B}"), Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val knights = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Creature.withSubtype("Knight")
                )
            )
            move(knights, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "{6}{W}{B}, Sacrifice this enchantment: Return all Knight creature cards from your " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "328"
        artist = "Paul Scott Canavan"
        flavorText = "\"By hoof, wing, or paw. For the realm!\"\n—Syr Gwyn, Hero of Ashvale"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b86f7db-ae1d-49fe-a9c5-488bbf3afac7.jpg?1783932547"
        ruling(
            "2019-10-04",
            "In a Two-Headed Giant game, Knights' Charge's first ability causes the opposing team to lose 1 life " +
                "twice, and you gain 1 life once."
        )
    }
}
