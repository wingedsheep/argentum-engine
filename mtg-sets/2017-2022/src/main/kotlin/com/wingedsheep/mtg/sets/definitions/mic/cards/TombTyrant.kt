package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tomb Tyrant — Innistrad: Midnight Hunt Commander #23 (canonical printing)
 * {3}{B} · Creature — Zombie Noble · 3/3
 *
 * Other Zombies you control get +1/+1.
 * {2}{B}, {T}, Sacrifice a creature: Return a Zombie creature card at random from your graveyard to
 * the battlefield. Activate only during your turn and only if there are at least three Zombie
 * creature cards in your graveyard.
 *
 * - The lord clause is the Lord of the Accursed shape (`excludeSelf = true` for "other").
 * - Both activation limits are [ActivationRestriction]s, checked as the ability is activated —
 *   before the sacrifice is paid — so the graveyard needs three Zombie creature cards *before*
 *   activating; sacrificing a Zombie can't supply the third (the card's ruling). A Zombie card
 *   sacrificed to the cost is in the graveyard by resolution and may be the one returned.
 * - "At random" is a gather → `chooseRandom(1)` → move pipeline over the graveyard at resolution.
 */
val TombTyrant = card("Tomb Tyrant") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Noble"
    power = 3
    toughness = 3
    oracleText = "Other Zombies you control get +1/+1.\n" +
        "{2}{B}, {T}, Sacrifice a creature: Return a Zombie creature card at random from your graveyard " +
        "to the battlefield. Activate only during your turn and only if there are at least three " +
        "Zombie creature cards in your graveyard."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.Pipeline {
            val zombieCards = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE),
                )
            )
            val returned = chooseRandom(1, from = zombieCards)
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.OnlyIfCondition(
                Conditions.CardsInGraveyardMatchingAtLeast(3, GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE))
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73315f75-252e-4db2-b48c-a60eafaf25e3.jpg?1783925375"
        ruling("2021-09-24", "You need at least three Zombie creature cards in your graveyard before you begin to activate Tomb Tyrant's activated ability. You can't begin to activate it with two and plan to sacrifice a Zombie for the third.")
        ruling("2021-09-24", "If the creature you sacrifice to pay the cost is a Zombie card, it will be among the cards that may be chosen.")
    }
}
